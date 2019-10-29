package mediaserver.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class IO {

    public static final ObjectMapper OM = new ObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final boolean dev;

    private static final String GRADLE_OUT = "out/production";

    public IO(boolean dev) {

        this.dev = dev;
    }

    public URL resolve(String path) {

        try {
            return URI.create((dev ? "http://localhost:8080" : "https://taninim.stuf.link") + path).toURL();
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Failed to resolve " + path, e);
        }
    }

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

        return readStream(path, readMapFrom(path));
    }

    public static Map<String, ?> readData(URI uri) {

        return tryReadStream(uri, readMapFrom(uri));
    }

    public static <T> T readStream(Path path, Function<InputStream, T> receptor) {

        try (InputStream fos = new BufferedInputStream(new FileInputStream(path.toFile()))) {
            return receptor.apply(fos);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not read from " + path, e);
        }
    }

    public static <T> T tryReadStream(URI uri, Function<InputStream, T> receptor) {

        HttpURLConnection urlConnection;
        try {
            urlConnection = (HttpURLConnection) uri.toURL().openConnection();
        } catch (Exception e) {
            throw new IllegalStateException("Made no sense of " + uri, e);
        }
        try (InputStream fos = new BufferedInputStream(urlConnection.getInputStream())) {
            return receptor.apply(fos);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not read from " + uri, e);
        } finally {
            urlConnection.disconnect();
        }
    }

    public Optional<String> read(String resource) {

        return readBytes(resource)
            .map(bytes -> new String(bytes, StandardCharsets.UTF_8));
    }

    public Optional<byte[]> readBytes(String resource) {

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

    private static Function<InputStream, Map<String, ?>> readMapFrom(Object source) {

        return is -> {
            try {
                return IO.OM.readerFor(Map.class).readValue(is);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read: " + source, e);
            }
        };
    }

    private Optional<InputStream> readStream(String resource) {

        Optional<URL> url = Optional.ofNullable(
            Thread.currentThread().getContextClassLoader().getResource(resource));
        if (url.isEmpty()) {
            throw new IllegalArgumentException("No such resource: " + resource);
        }
        if (dev && url.filter(this::existsAsSource).isPresent()) {
            return url.map(this::fromSourceEnvironment).orElseThrow(() ->
                new IllegalArgumentException(
                    "No such resource: " + resource));
        }
        return Optional.ofNullable(Thread.currentThread().getContextClassLoader().getResourceAsStream(resource));
    }

    private boolean existsAsSource(URL u) {

        return u.getFile().contains(GRADLE_OUT);
    }

    private Optional<InputStream> fromSourceEnvironment(URL url) {

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
