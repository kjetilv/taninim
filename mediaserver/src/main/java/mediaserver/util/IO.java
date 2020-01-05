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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class IO {

    public static final ObjectMapper OM = new ObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public static final ObjectMapper OMP = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public static final String ROOT = "/";

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

    public static ACL readLocalACL(String resource) {

        return read(resource)
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

    public static Sourced<String> read(String resource) {

        return readBytes(resource)
            .map(bytes ->
                new String(bytes, StandardCharsets.UTF_8));
    }

    public static Sourced<byte[]> readBytes(String resource) {

        return Sourced.readStream(resource)
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

    @SuppressWarnings("unchecked")
    public static Map<String, ?> readMap(Object source, InputStream is) {

        return read(Map.class, source, is);
    }

    public static <T> T read(Class<T> type, Object source, String data) {
        try {
            return IO.OM.readerFor(type).readValue(data);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read: " + source, e);
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
