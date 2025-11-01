package taninim.music.legal;

import module java.base;
import com.github.kjetilv.uplift.s3.S3Accessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taninim.music.aural.Chunk;
import taninim.music.medias.MediaLibrary;

import static java.util.Objects.requireNonNull;

public final class CloudMediaLibrary implements MediaLibrary {

    private static final Logger log = LoggerFactory.getLogger(CloudMediaLibrary.class);

    private final Map<String, Cached<Map<String, S3Accessor.RemoteInfo>>> infos = new ConcurrentHashMap<>();

    private final Map<String, Cached<byte[]>> fileCache = new ConcurrentHashMap<>();

    private final S3Accessor s3;

    private final Supplier<Instant> time;

    public static MediaLibrary create(S3Accessor s3, Supplier<Instant> time) {
        return new CloudMediaLibrary(s3, time);
    }

    public CloudMediaLibrary(S3Accessor s3, Supplier<Instant> time) {
        this.s3 = requireNonNull(s3, "s3");
        this.time = time;
    }

    @Override
    public Optional<Long> fileSize(String file) {
        var update = infosWithPrefix(file);
        return Optional.ofNullable(update.get(file))
            .map(S3Accessor.RemoteInfo::size);
    }

    @Override
    public Optional<? extends InputStream> stream(String file) {
        return infoWithPrefix(file).flatMap(info -> {
            var lastCached = fileCache.get(file);
            return needsUpdate(info, lastCached)
                ? update(time.get(), file, info, getLastValidTime(lastCached))
                : lastCached.optionalData()
                    .map(ByteArrayInputStream::new);
        });
    }

    @Override
    public Optional<? extends InputStream> stream(Chunk chunk, String file) {
        return s3.stream(file, chunk == null ? null : chunk.range());
    }

    @Override
    public void write(String file, Consumer<? super OutputStream> writer) {
        try (var baos = new ByteArrayOutputStream()) {
            writer.accept(baos);
            baos.close();
            try (InputStream inputStream = new ByteArrayInputStream(baos.toByteArray())) {
                s3.put(file, inputStream, baos.size());
            } finally {
                fileCache.put(file, new Cached<>(time.get(), baos.toByteArray()));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write " + file + " to " + writer, e);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[infos:" + infos.size() + " fileCache:" + fileCache.size() + "]";
    }

    private Optional<ByteArrayInputStream> update(
        Instant currentTime,
        String file,
        S3Accessor.RemoteInfo info,
        Instant lastValid
    ) {
        if (info.size() == 0) {
            log.debug("Empty file: {}", file);
            return Optional.empty();
        }
        Supplier<Optional<byte[]>> optionalSupplier = () ->
            s3.stream(file, null)
                .map(inputStream -> bytes(info, inputStream));
        return update(
            fileCache,
            file,
            currentTime,
            lastValid,
            optionalSupplier
        ).map(ByteArrayInputStream::new);
    }

    private Optional<S3Accessor.RemoteInfo> infoWithPrefix(String file) {
        return Optional.ofNullable(infosWithPrefix(file).get(file));
    }

    private Map<String, S3Accessor.RemoteInfo> infosWithPrefix(String file) {
        var prefix = file.substring(0, 1);
        var time = this.time.get();
        return update(infos, prefix, time, time.plus(TIMEOUT), () ->
            Optional.ofNullable(s3.remoteInfos(prefix))
        ).orElseGet(Collections::emptyMap);
    }

    private static final Duration TIMEOUT = Duration.ofMinutes(5);

    private static Instant getLastValidTime(Cached<byte[]> lastCached) {
        return lastCached == null ? null : lastCached.time().plus(TIMEOUT);
    }

    private static byte[] bytes(S3Accessor.RemoteInfo info, InputStream inputStream) {
        var size = Math.toIntExact(info.size());
        var bytes = new byte[size];
        var fillIndex = 0;
        try {
            while (true) {
                var bytesRead = inputStream.read(bytes, fillIndex, size - fillIndex);
                fillIndex += bytesRead;
                if (fillIndex >= size) {
                    return bytes;
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read " + info, e);
        }
    }

    private static boolean needsUpdate(S3Accessor.RemoteInfo info, Cached<?> lastCached) {
        return lastCached == null || info.lastModified().isAfter(lastCached.time());
    }

    private static <T> Optional<T> update(
        Map<? super String, Cached<T>> map,
        String key,
        Instant time,
        Instant lastValidTime,
        Supplier<Optional<T>> refresher
    ) {
        return Optional.ofNullable(
                map.compute(key, (_, cached) ->
                    Optional.ofNullable(cached)
                        .filter(c ->
                            !c.outdatedAt(lastValidTime))
                        .or(() ->
                            refresher.get()
                                .map(t ->
                                    new Cached<>(time, t)))
                        .orElse(null))
            )
            .map(Cached::data);
    }

    private record Cached<T>(
        Instant time,
        T data
    ) {

        private boolean outdatedAt(Instant lastValidTime) {
            return lastValidTime != null && time().isAfter(lastValidTime);
        }

        private Optional<T> optionalData() {
            return Optional.ofNullable(data());
        }
    }
}
