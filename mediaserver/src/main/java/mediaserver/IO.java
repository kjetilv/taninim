package mediaserver;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

final class IO {

    private IO() {
    }

    static Optional<String> read(String resource) {
        return readBytes(resource)
            .map(bytes -> new String(bytes, StandardCharsets.UTF_8));
    }

    static Optional<byte[]> readBytes(String resource) {
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

    static Map<String, String> queryParams(String pars) {
        int index = 0;
        Map<String, String> map = new HashMap<>();
        while (true) {
            int nextPair = pars.indexOf("&", index);
            boolean last = nextPair <= 0;
            if (last) {
                nextPair = pars.length();
            }
            int eqIndex = pars.indexOf("=", index);
            if (eqIndex < 0 || eqIndex > nextPair) {
                throw new IllegalStateException("Expected value for " + pars.substring(index + 1, nextPair));
            }
            map.put(pars.substring(index, eqIndex), pars.substring(eqIndex + 1, nextPair));
            if (last) {
                return map;
            }
            index = nextPair + 1;
        }
    }

    private static Optional<InputStream> stream(String resource) {
        return Optional.ofNullable(
            Thread.currentThread().getContextClassLoader().getResource(resource))
            .map(URL::getFile)
            .map(name ->
                name.replaceAll("out/production", "src/main"))
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
