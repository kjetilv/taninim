package mediaserver.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import static mediaserver.util.IO.Type.JAR;
import static mediaserver.util.IO.Type.SOURCES;
import static mediaserver.util.IO.Type.UNKNOWN;

public final class IO {
    
    public static final ObjectMapper OM = new ObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    
    public static final ObjectMapper OMP = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    
    public static <T> T writeStream(Path path, T output, BiConsumer<? super T, ? super OutputStream> receptor) {
        if (path.getParent().toFile().isDirectory() || path.getParent().toFile().mkdirs()) {
            try (
                FileOutputStream fos = new FileOutputStream(path.toFile())
            ) {
                receptor.accept(output, fos);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to write to " + path, e);
            }
            return output;
        }
        throw new IllegalStateException("Could not verify dir: " + path);
    }
    
    public static Map<String, ?> readData(Path path) {
        return readFromStream(path, is -> readMap(path, is));
    }
    
    @SafeVarargs
    public static Map<String, ?> downloadJson(URI uri, Consumer<BiConsumer<String, String>>... headers) {
        return tryDownload(uri, is -> readMap(uri, is), headers);
    }
    
    @SafeVarargs
    public static byte[] download(URI uri, Consumer<BiConsumer<String, String>>... headers) {
        return tryDownload(uri, is -> readBytesFrom(uri, is), headers);
    }
    
    public static <T> T readFromStream(Path path, Function<? super InputStream, T> receptor) {
        try (InputStream fos = new BufferedInputStream(new FileInputStream(path.toFile()))) {
            return receptor.apply(fos);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not read from " + path, e);
        }
    }
    
    public static Sourced<String> readUTF8(String resource) {
        return readBytes(resource)
            .map(bytes ->
                new String(bytes, StandardCharsets.UTF_8));
    }
    
    public static Sourced<byte[]> readBytes(String resource) {
        return readStream(resource)
            .map(stream -> readBytesFrom(resource, stream));
    }
    
    public static Sourced<Stream<String>> readLines(String resource) {
        return readStream(resource)
            .map(stream -> readLinesFrom(resource, stream));
    }
    
    public static <T> T readObject(Class<T> type, String input) {
        try {
            return OM.readerFor(type).readValue(input);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to read " + type, e);
        }
    }
    
    public static <T> T read(Class<T> type, Object source, InputStream is) {
        try {
            return IO.OM.readerFor(type).readValue(is);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read: " + source, e);
        }
    }
    
    public static String getProperty(String property) {
        return System.getProperty(property, System.getenv(property));
    }
    
    public static Collection<Path> paths(String resource) {
        return sourceUrl(resource)
            .filter(url -> type(url) == SOURCES)
            .map(URL::getFile)
            .map(File::new)
            .map(File::listFiles)
            .stream()
            .map(Arrays::stream)
            .flatMap(files ->
                files.map(File::toPath))
            .collect(Collectors.toList());
    }
    
    public static <T> T read(Class<T> type, Object source, String data) {
        try {
            return IO.OM.readerFor(type).readValue(data);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read: " + source, e);
        }
    }
    
    public static <T> Function<T, Sourced<T>> from(Type source, URL url) {
        return t -> new Sourced<>(source, t, url);
    }
    
    static <T> Sourced<T> from(Type source, T t, URL url) {
        return new Sourced<>(source, t, url);
    }
    
    public enum Type {
        SOURCES, JAR, UNKNOWN
    }
    
    private IO() {
    }
    
    private static final int OK = 200;
    
    private static final int ATE_KAY = 8192;
    
    private static final String GRADLE_OUT = "out/production";
    
    private static final String JAR_BANG = "jar!";
    
    private static final Pattern GRADLE_OUT_PATTERN = Pattern.compile(GRADLE_OUT);
    
    private static final String SRC_MAIN = "src/main";
    
    private static Type type(URL sourceUrl) {
        return isInSources(sourceUrl) ? SOURCES
            : isInTheJar(sourceUrl) ? JAR
                : UNKNOWN;
    }
    
    private static Sourced<InputStream> readStream(String resource) {
        URL sourceUrl = sourceUrl(resource).orElseThrow(() ->
            new IllegalArgumentException("No such resource: " + resource));
        if (isInSources(sourceUrl)) {
            URL adjusted = inSources(sourceUrl);
            return from(SOURCES, readSources(resource, adjusted), adjusted);
        }
        if (isInTheJar(sourceUrl)) {
            return from(JAR, readClasspath(resource), sourceUrl);
        }
        throw new IllegalStateException("Unknown resource location: " + sourceUrl);
    }
    
    private static Stream<String> readLinesFrom(String resource, InputStream stream) {
        @SuppressWarnings({ "resource", "IOResourceOpenedButNotSafelyClosed" })
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        AtomicReference<String> line = new AtomicReference<>();
        line.set(readLineFrom(resource, reader));
        if (line.get() == null) {
            return Stream.empty();
        }
        return StreamSupport.stream(new Spliterators.AbstractSpliterator<>(Long.MAX_VALUE, Spliterator.IMMUTABLE) {
            
            @Override
            public boolean tryAdvance(Consumer<? super String> action) {
                action.accept(line.get());
                line.set(readLineFrom(resource, reader));
                boolean done = line.get() == null;
                if (done) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        throw new IllegalStateException("Failed to close " + resource, e);
                    }
                }
                return !done;
            }
        }, false);
    }
    
    private static String readLineFrom(String resource, BufferedReader bufferedReader) {
        try {
            return bufferedReader.readLine();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read from " + resource, e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private static Map<String, ?> readMap(Object source, InputStream is) {
        return (Map<String, ?>) read(Map.class, source, is);
    }
    
    @SafeVarargs
    private static <T> T tryDownload(
        URI uri,
        Function<? super InputStream, T> receptor,
        Consumer<BiConsumer<String, String>>... headers
    ) {
        HttpURLConnection urlConnection;
        try {
            urlConnection = (HttpURLConnection) uri.toURL().openConnection();
        } catch (Exception e) {
            throw new IllegalStateException("Made no sense of " + uri, e);
        }
        for (Consumer<BiConsumer<String, String>> header: headers) {
            header.accept(urlConnection::setRequestProperty);
        }
        try {
            int responseCode;
            try {
                responseCode = urlConnection.getResponseCode();
            } catch (IOException e) {
                throw new IllegalStateException("Could not assert response code from " + uri, e);
            }
            if (responseCode != OK) {
                return fail(responseCode, urlConnection, uri);
            }
            return response(uri, receptor, urlConnection);
        } finally {
            urlConnection.disconnect();
        }
    }
    
    private static <T> T response(URI uri, Function<? super InputStream, T> receptor, HttpURLConnection urlConnection) {
        try (InputStream fos = new BufferedInputStream(urlConnection.getInputStream())) {
            return receptor.apply(fos);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not read from " + uri, e);
        }
    }
    
    private static <T> T fail(int responseCode, HttpURLConnection urlConnection, URI uri) {
        try (
            InputStream fos = new BufferedInputStream(urlConnection.getErrorStream());
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fos, StandardCharsets.UTF_8))
        ) {
            String error;
            try {
                error = bufferedReader.lines().collect(Collectors.joining("\n"));
            } catch (Exception e) {
                error = "Failed to read error information:" + e;
            }
            throw new IllegalStateException(responseCode + ": Failed to open " + uri + " " + error);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read error resposne from " + uri, e);
        }
    }
    
    private static byte[] readBytesFrom(Object resource, InputStream stream) {
        byte[] buf = new byte[ATE_KAY];
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            return readTo(stream, buf, baos);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read " + resource, e);
        }
    }
    
    private static byte[] readTo(InputStream stream, byte[] buf, ByteArrayOutputStream baos) {
        int bytesRead = 0;
        try {
            while (true) {
                int read = stream.read(buf);
                if (read < 0) {
                    bytesRead += read;
                    baos.flush();
                    return baos.toByteArray();
                }
                baos.write(buf, 0, read);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Read failed after " + bytesRead + " bytes", e);
        }
    }
    
    private static Optional<URL> sourceUrl(String resource) {
        return Optional.ofNullable(
            Thread.currentThread().getContextClassLoader().getResource(resource));
    }
    
    private static URL inSources(URL sourceUrl) {
        try {
            return URI.create(
                GRADLE_OUT_PATTERN.matcher(sourceUrl.toExternalForm())
                    .replaceAll(SRC_MAIN)).toURL();
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Could not transform to sources url: " + sourceUrl, e);
        }
    }
    
    @Nullable
    private static InputStream readClasspath(String resource) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
    }
    
    private static InputStream readSources(String resource, URL url) {
        return fromSourceEnvironment(url).orElseThrow(() ->
            new IllegalArgumentException("No such resource: " + resource));
    }
    
    private static boolean isInSources(URL url) {
        return url.getFile().contains(GRADLE_OUT);
    }
    
    private static boolean isInTheJar(URL url) {
        return url.getFile().contains(JAR_BANG);
    }
    
    private static Optional<InputStream> fromSourceEnvironment(URL url) {
        return Optional.of(url)
            .map(URL::getFile)
            .map(GRADLE_OUT_PATTERN::matcher)
            .map(IO::toSourceName)
            .map(sourceName -> {
                try {
                    return new FileInputStream(sourceName);
                } catch (FileNotFoundException e) {
                    throw new IllegalStateException("Failed to open file: " + sourceName, e);
                }
            });
    }
    
    private static String toSourceName(Matcher matcher) {
        return matcher.replaceAll(SRC_MAIN);
    }
}
