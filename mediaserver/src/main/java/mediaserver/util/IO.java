package mediaserver.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static mediaserver.util.Sourced.Type.JAR;
import static mediaserver.util.Sourced.Type.SOURCES;

public final class IO {

    public static final ObjectMapper OM = new ObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public static final ObjectMapper OMP = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public static final String ROOT = "/";

    private static final String GRADLE_OUT = "out/production";

    public static <T> void writeStream(Path path, T output, BiConsumer<T, OutputStream> receptor) {

        if (!(path.getParent().toFile().isDirectory() || path.getParent().toFile().mkdirs())) {
            throw new IllegalStateException("Could not verify dir: " + path);
        }
        try (
            FileOutputStream fos = new FileOutputStream(path.toFile())
        ) {
            receptor.accept(output, fos);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to write to " + path, e);
        }
    }

    public static Map<String, ?> readData(Path path) {

        return readFromStream(path, readMapFrom(path));
    }

    public static Map<String, ?> downloadJson(URI uri, Consumer<BiConsumer<String, String>> headers) {

        return tryDownload(uri, headers, readMapFrom(uri));
    }

    public static Map<String, String> readResource(String resource) {

        return Optional.ofNullable(readClasspath(resource))
            .map(res ->
                readMapFrom(resource).apply(res))
            .map(map ->
                map.entrySet().stream().collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue().toString(),
                    (s1, s2) -> {
                        throw new IllegalStateException("No combine:" + s1 + "/" + s2);
                    },
                    LinkedHashMap::new
                )))
            .orElseThrow(() ->
                new IllegalArgumentException("No resource found @ " + resource));
    }

    public static <T> T readFromStream(Path path, Function<InputStream, T> receptor) {

        try (InputStream fos = new BufferedInputStream(new FileInputStream(path.toFile()))) {
            return receptor.apply(fos);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not read from " + path, e);
        }
    }

    public static Sourced<String> read(String resource) {

        return readBytes(resource)
            .map(bytes ->
                new String(bytes, StandardCharsets.UTF_8));
    }

    public static Sourced<byte[]> readBytes(String resource) {

        return readStream(resource)
            .map(stream -> {
                byte[] buf = new byte[8192];
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    return readTo(stream, buf, baos);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to read " + resource, e);
                }
            });
    }

    public static <T> T readObject(Class<T> type, String input) {

        try {
            return OM.readerFor(type).readValue(input);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to read " + type, e);
        }
    }

    public static Map<String, ?> readMap(Object source, InputStream is) {

        try {
            return IO.OM.readerFor(Map.class).readValue(is);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read: " + source, e);
        }
    }

    public static String getProperty(String property) {

        return System.getProperty(property, System.getenv(property));
    }

    private static <T> T tryDownload(
        URI uri,
        Consumer<BiConsumer<String, String>> headers,
        Function<InputStream, T> receptor
    ) {

        HttpURLConnection urlConnection;
        try {
            urlConnection = (HttpURLConnection) uri.toURL().openConnection();
        } catch (Exception e) {
            throw new IllegalStateException("Made no sense of " + uri, e);
        }
        if (headers != null) {
            headers.accept(urlConnection::setRequestProperty);
        }
        try {
            int responseCode;
            try {
                responseCode = urlConnection.getResponseCode();
            } catch (IOException e) {
                throw new IllegalStateException("Could not assert response code from " + uri, e);
            }
            if (responseCode == 200) {
                try (InputStream fos = new BufferedInputStream(urlConnection.getInputStream())) {
                    return receptor.apply(fos);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Could not read from " + uri, e);
                }
            }
            try (InputStream fos = new BufferedInputStream(urlConnection.getErrorStream())) {
                throw new IllegalStateException("Failed to open " + uri + " " + error(fos));
            } catch (IOException e) {
                throw new IllegalStateException("Could not read error resposne from " + uri, e);
            }
        } finally {
            urlConnection.disconnect();
        }
    }

    private static String error(InputStream fos) {

        return new BufferedReader(new InputStreamReader(fos)).lines().collect(Collectors.joining("\n"));
    }

    private static Function<InputStream, Map<String, ?>> readMapFrom(Object source) {

        return is -> readMap(source, is);
    }

    private static Sourced<InputStream> readStream(String resource) {

        return url(resource)
            .filter(IO::isInSources)
            .map(sourceUrl ->
                IO.isInSources(sourceUrl)
                    ? Sourced.from(SOURCES, readSources(resource, sourceUrl), sourceUrl)
                    : Sourced.from(JAR, readClasspath(resource), sourceUrl))
            .orElseGet(
                Sourced::notFound);
    }

    private static InputStream readClasspath(String resource) {

        return Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
    }

    private static InputStream readSources(String resource, URL url) {

        return fromSourceEnvironment(url).orElseThrow(() ->
            new IllegalArgumentException("No such resource: " + resource));
    }

    private static Optional<URL> url(String resource) {

        Optional<URL> url = Optional.ofNullable(
            Thread.currentThread().getContextClassLoader().getResource(resource));
        if (url.isEmpty()) {
            throw new IllegalArgumentException("No such resource: " + resource);
        }
        return url;
    }

    private static boolean isInSources(URL url) {

        return url.getFile().contains(GRADLE_OUT);
    }

    private static Optional<InputStream> fromSourceEnvironment(URL url) {

        return Optional.of(url)
            .map(URL::getFile)
            .map(name ->
                name.replaceAll(GRADLE_OUT, "src/main"))
            .map(name -> {
                try {
                    return new FileInputStream(name);
                } catch (FileNotFoundException e) {
                    throw new IllegalStateException("Failed to open file: " + name, e);
                }
            });
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
}
