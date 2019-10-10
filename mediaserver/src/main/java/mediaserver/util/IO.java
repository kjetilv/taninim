package mediaserver.util;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public final class IO {

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

    public Optional<String> read(String resource) {
        return readBytes(resource)
            .map(bytes -> new String(bytes, StandardCharsets.UTF_8));
    }

    public Optional<byte[]> readBytes(String resource) {
        return stream(resource)
            .map(stream -> {
                byte[] buf = new byte[8192];
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    return readTo(stream, buf, baos);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to read " + resource, e);
                }
            });
    }

    private Optional<InputStream> stream(String resource) {
        Optional<URL> url = Optional.ofNullable(
            Thread.currentThread().getContextClassLoader().getResource(resource));
        if (url.isEmpty()) {
            throw new IllegalArgumentException("No such resource: " + resource);
        }
        if (dev && url.filter(this::existsAsSource).isPresent()) {
            return url.map(this::fromSourceEnvironment).orElseThrow(() ->
                new IllegalArgumentException("No such resource: " + resource));
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
