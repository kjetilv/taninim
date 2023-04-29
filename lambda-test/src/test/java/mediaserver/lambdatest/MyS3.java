package mediaserver.lambdatest;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.github.kjetilv.uplift.kernel.io.BytesIO;
import com.github.kjetilv.uplift.kernel.io.Range;
import com.github.kjetilv.uplift.s3.S3Accessor;
import mediaserver.taninim.music.medias.MediaIds;
import mediaserver.taninim.music.medias.UserAuths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record MyS3(
    Map<String, S3Data> s3,
    Supplier<Instant> time
) implements S3Accessor {

    private static final Logger log = LoggerFactory.getLogger(MyS3.class);

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
    public Optional<InputStream> put(String remoteName, InputStream inputStream, long length) {
        log.info("Putting {} bytes into {}", length, remoteName);
        byte[] bytes = BytesIO.readInputStream(inputStream);
        s3.put(remoteName, new S3Data(
            bytes,
            stringValue(remoteName, bytes),
            time.get()
        ));
        return Optional.of(inputStream);
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
