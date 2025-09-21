package taninim.lambdatest;

import module java.base;
import module taninim.taninim;
import module uplift.flogs;
import module uplift.kernel;
import module uplift.s3;
import org.slf4j.Logger;

public record MemoryS3(
    Map<String, S3Data> s3,
    Supplier<Instant> time
) implements S3Accessor {

    private static final Logger log = LoggerFactory.getLogger(MemoryS3.class);

    @Override
    public Optional<? extends InputStream> stream(String name, Range range) {
        return Optional.ofNullable(s3.get(name)).map(data ->
            range == null
                ? new ByteArrayInputStream(data.data())
                : new ByteArrayInputStream(
                    data.data(),
                    Math.toIntExact(range.start()),
                    Math.toIntExact(range.exclusiveEnd() - range.start())
                ));
    }

    @Override
    public void put(String remoteName, InputStream inputStream, long length) {
        log.info("Putting {} bytes into {}", length, remoteName);
        byte[] bytes = BytesIO.readInputStream(inputStream);
        s3.put(remoteName, new S3Data(
            bytes,
            stringValue(remoteName, bytes),
            time.get()
        ));
    }

    @Override
    public Map<String, RemoteInfo> remoteInfos(String prefix) {
        return s3.entrySet()
            .stream()
            .filter(e -> e.getKey().startsWith(prefix))
            .map(e -> Map.entry(
                e.getKey(),
                new RemoteInfo(e.getKey(), e.getValue().time(), e.getValue().data().length)
            ))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public void remove(Collection<String> objects) {
        for (String object: objects) {
            log.info("Removing {}", object);
            s3.remove(object);
        }
    }

    private static String stringValue(String remoteName, byte[] bytes) {
        if (remoteName.startsWith("auth-digest")) {
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                return UserAuths.from(input).toString();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        if (remoteName.startsWith("media-digest")) {
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                return MediaIds.from(input).toString();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        if (remoteName.endsWith("m4a")) {
            return "[audio]";
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public record S3Data(
        byte[] data,
        String str,
        Instant time
    ) {

    }
}
