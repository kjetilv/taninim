package mediaserver.externals;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import mediaserver.media.Album;
import mediaserver.media.Media;
import mediaserver.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DiscogsDataResolver {

    private static final Logger log = LoggerFactory.getLogger(DiscogsDataResolver.class);

    private final Path resourcesDirectory;

    private final Map<Album, DiscogConnection> connections;

    private final Duration refreshTime;

    private final Clock clock;

    private final AtomicBoolean discogsTired = new AtomicBoolean();

    private final AtomicInteger discogsTries = new AtomicInteger(10);

    public DiscogsDataResolver(
        Path resourcesDirectory,
        Collection<DiscogConnection> connections,
        Duration refreshTime,
        Clock clock
    ) {

        this.resourcesDirectory = resourcesDirectory;
        this.connections =
            connections == null || connections.isEmpty() ? Collections.emptyMap() : connections.stream()
                .collect(Collectors.toMap(
                    DiscogConnection::getAlbum,
                    Function.identity()
                ));
        this.refreshTime = refreshTime;
        this.clock = clock;
    }

    public Map<Album, DiscogConnection> getConnections() {

        return Collections.unmodifiableMap(connections);
    }

    public Optional<DiscogReleaseDigest> getDiscogRelease(Album album) {
        return Optional.ofNullable(connections.get(album))
            .filter(meta ->
                match(album, meta))
            .flatMap(connection ->
                digest(
                    connection,
                    pathTo(connection, ".json"),
                    pathTo(connection, "-raw.json")
                ).map(updateCover(
                    pathTo(connection, ".jpg"))
                ));
    }

    private Optional<DiscogReleaseDigest> digest(DiscogConnection connection, Path local, Path raw) {

        return withRetry(3, () -> {
            try {
                if (regularFile(local) && regularFile(raw) && fresh(raw)) {
                    return updateAndReadLocalFile(raw, local);
                }
                try {
                    if (discogsTired.get() || discogsTries.get() <= 0) {
                        return Optional.empty();
                    }
                    Optional<DiscogReleaseDigest> digest =
                        fetchAndWriteLocalFile(connection.getUri(), local, raw);
                    log.debug("Re-read from discogs: {} -> {}", connection, digest.orElse(null));
                    return digest;
                } catch (Exception e) {
                    log.warn("Discog read problem for {}/{}, continuing...", local, raw, e);
                    discogsTired.set(true);
                }
            } catch (Exception e) {
                log.warn("Failed to retrieve data for {}, proceeding", connection, e);
            }
            return Optional.empty();
        });
    }

    private Path pathTo(DiscogConnection connection, String suffix) {

        return resourcesDirectory.resolve(
            Paths.get(connection.getType(), connection.getId() + suffix));
    }

    private boolean fresh(Path raw) {

        try {
            return Duration.between(
                modifiedTime(raw),
                clock.instant()
            ).minus(refreshTime).isNegative();
        } catch (IOException e) {
            log.warn("Could not assert age of {}", raw, e);
            return false;
        }
    }

    private Optional<DiscogReleaseDigest> fetchAndWriteLocalFile(
        URI uri,
        Path localDigestPath,
        Path localRawPath
    ) {
        if (discogsTries.decrementAndGet() > 0) {
            Map<String, ?> rawData = IO.downloadJson(uri, AUTHORIZATION);
            DiscogReleaseDigest digest =
                IO.writeStream(localDigestPath, readRelease(rawData), writeRelease(DiscogReleaseDigest.class));
            IO.writeStream(localRawPath, rawData, writeRelease(Map.class));
            log.debug("Updated local digest: {} <== {}", digest.getTitle(), uri);
            return Optional.of(digest);
        }
        return Optional.empty();
    }

    private static final String KEY = "jTeXCJgjPPGaXAOQHkqS";

    private static final String SECRET = IO.getProperty("dSec");

    private static final Consumer<BiConsumer<String, String>> AUTHORIZATION = headers ->
        headers.accept("Authorization", "Discogs key=" + KEY + ", secret=" + SECRET);

    private static final byte[] NO_DATA = { };

    private static boolean match(Album album, DiscogConnection meta) {

        return meta.isUp() && meta.getAlbum().equals(album);
    }

    private static Function<DiscogReleaseDigest, DiscogReleaseDigest> updateCover(Path cover) {

        return digest -> {
            if (cover.toFile().exists()) {
                return digest;
            }
            byte[] bytes = Media.getCover(digest, DiscogImage::getUri)
                .map(IO::download)
                .orElse(NO_DATA);
            IO.writeStream(cover, bytes, (bytes1, outputStream) -> {
                try {
                    outputStream.write(bytes1);
                } catch (IOException e) {
                    throw new IllegalArgumentException(
                        "Failed to write " + bytes.length + " bytes to " + cover, e);
                }
            });
            return digest;
        };
    }

    @SuppressWarnings("SameParameterValue")
    private static <T> Optional<T> withRetry(int retries, Supplier<Optional<T>> fun) {

        return IntStream.range(0, retries).mapToObj(retry -> fun.get()).flatMap(Optional::stream).findFirst();
    }

    private static boolean regularFile(Path local) {

        try {
            return Files.isRegularFile(local) && Files.size(local) > 0;
        } catch (Exception e) {
            log.warn("Failed to size " + local + ", treating as irregular", e);
            return false;
        }
    }

    private static Optional<DiscogReleaseDigest> updateAndReadLocalFile(Path raw, Path local) {

        Map<String, ?> rawData = IO.readData(raw);
        DiscogReleaseDigest digest = readRelease(rawData);
        try {
            if (modifiedTime(raw).isAfter(modifiedTime(local))) {
                IO.writeStream(local, digest, writeRelease(DiscogReleaseDigest.class));
            }
        } catch (IOException e) {
            log.warn("Failed comparison of files {}/{}, rewriting", local, raw, e);
            IO.writeStream(local, digest, writeRelease(DiscogReleaseDigest.class));
        }
        return Optional.of(digest);
    }

    private static Instant modifiedTime(Path local)
        throws IOException {

        return Files.getLastModifiedTime(local).toInstant();
    }

    private static DiscogReleaseDigest readRelease(Map<String, ?> rawData) {

        try {
            return IO.OM.readerFor(DiscogReleaseDigest.class).readValue(
                IO.OM.writerFor(Map.class).writeValueAsBytes(rawData));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read", e);
        }
    }

    private static <T> BiConsumer<T, OutputStream> writeRelease(Class<T> type) {

        return (data, os) -> {
            try {
                IO.OM.writerFor(type).writeValue(os, data);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read", e);
            }
        };
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[@" + resourcesDirectory + "]";
    }
}
