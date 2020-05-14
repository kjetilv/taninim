package mediaserver.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import mediaserver.externals.ACL;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class IO {

    public static final ObjectMapper OM = new ObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public static final ObjectMapper OMP = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private IO() {

    }

    public static <T> T writeStream(Path path, T output, BiConsumer<T, OutputStream> receptor) {

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

    public static ACL readLocalACL(String resource) {

        return readUTF8(resource)
            .unpack(res ->
                read(ACL.class, resource, res))
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

    public static Sourced<String> readUTF8(String resource) {

        return readBytes(resource)
            .map(bytes ->
                new String(bytes, StandardCharsets.UTF_8));
    }

    public static Sourced<byte[]> readBytes(String resource) {

        return Sourced.readStream(resource)
            .map(stream -> readBytesFrom(resource, stream));
    }

    public static Sourced<Stream<String>> readLines(String resource) {

        return Sourced.readStream(resource)
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

    private static Stream<String> readLinesFrom(String resource, InputStream stream) {

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));

        AtomicReference<String> line = new AtomicReference<>();
        line.set(readLineFrom(resource, bufferedReader));
        if (line.get() == null) {
            return Stream.empty();
        }

        return StreamSupport.stream(new Spliterators.AbstractSpliterator<>(Long.MAX_VALUE, Spliterator.IMMUTABLE) {

            @Override
            public boolean tryAdvance(Consumer<? super String> action) {

                action.accept(line.get());
                line.set(readLineFrom(resource, bufferedReader));
                return line.get() != null;
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

        return read(Map.class, source, is);
    }

    private static <T> T read(Class<T> type, Object source, String data) {

        try {
            return IO.OM.readerFor(type).readValue(data);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read: " + source, e);
        }
    }

    private static <T> T tryDownload(
        URI uri,
        Function<InputStream, T> receptor,
        Consumer<BiConsumer<String, String>>... headers
    ) {

        HttpURLConnection urlConnection;
        try {
            urlConnection = (HttpURLConnection) uri.toURL().openConnection();
        } catch (Exception e) {
            throw new IllegalStateException("Made no sense of " + uri, e);
        }
        for (Consumer<BiConsumer<String, String>> header : headers) {
            header.accept(urlConnection::setRequestProperty);
        }
        try {
            int responseCode;
            try {
                responseCode = urlConnection.getResponseCode();
            } catch (IOException e) {
                throw new IllegalStateException("Could not assert response code from " + uri, e);
            }
            if (responseCode != 200) {
                return fail(responseCode, urlConnection, uri);
            }
            return response(uri, receptor, urlConnection);
        } finally {
            urlConnection.disconnect();
        }
    }

    private static <T> T response(URI uri, Function<InputStream, T> receptor, HttpURLConnection urlConnection) {

        try (InputStream fos = new BufferedInputStream(urlConnection.getInputStream())) {
            return receptor.apply(fos);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not read from " + uri, e);
        }
    }

    private static <T> T fail(int responseCode, HttpURLConnection urlConnection, URI uri) {

        try (InputStream fos = new BufferedInputStream(urlConnection.getErrorStream());
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fos))) {
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

        byte[] buf = new byte[8192];
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
}
