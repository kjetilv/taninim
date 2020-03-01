package mediaserver.externals;

import mediaserver.media.Album;
import mediaserver.media.Media;
import mediaserver.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public final class DiscogsDataResolver {

    private static final Logger log = LoggerFactory.getLogger(DiscogsDataResolver.class);

    private static final String KEY = "jTeXCJgjPPGaXAOQHkqS";

    private static final String SECRET = IO.getProperty("dSec");

    private static final Consumer<BiConsumer<String, String>> AUTHORIZATION = headers ->
        headers.accept("Authorization", "Discogs key=" + KEY + ", secret=" + SECRET);

    private final Path resourcesDirectory;

    private final Collection<DiscogConnection> connections;

    private final Duration refreshTime;

    private final Clock clock;

    private final AtomicBoolean discogsTired = new AtomicBoolean();

    static final byte[] NO_DATA = {};

    public DiscogsDataResolver(
        Path resourcesDirectory,
        Collection<DiscogConnection> connections,
        Duration refreshTime,
        Clock clock
    ) {

        this.resourcesDirectory = resourcesDirectory;
        this.connections = connections;
        this.refreshTime = refreshTime;
        this.clock = clock;
    }

    public Collection<DiscogConnection> getConnections() {

        return List.copyOf(connections);
    }

    public Optional<DiscogReleaseDigest> getDiscogRelease(Album album) {

        return connections.stream()
            .filter(meta ->
                match(album, meta))
            .map(connection ->
                digest(
                    connection,
                    pathTo(connection, ".json"),
                    pathTo(connection, "-raw.json")
                ).map(updateCover(
                    pathTo(connection, ".jpg"))
                ))
            .flatMap(Optional::stream)
            .findFirst();
    }

    private static Function<DiscogReleaseDigest, DiscogReleaseDigest> updateCover(Path cover) {

        return digest -> {
            if (cover.toFile().exists()) {
                return digest;
            }
            byte[] bytes = Media.getCover(digest, DiscogImage::getUri)
                .map(uri ->
                    IO.download(uri))
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

    private Path pathTo(DiscogConnection connection, String suffix) {

        return resourcesDirectory.resolve(
            Paths.get(connection.getType(), connection.getId() + suffix));
    }

    private Optional<DiscogReleaseDigest> digest(DiscogConnection connection, Path local, Path raw) {

        return withRetry(3, () -> {
            try {
                if (regularFile(local) && regularFile(raw) && fresh(raw) || discogsTired.get()) {
                    return updateAndReadLocalFile(raw, local);
                }
                try {
                    return fetchAndWriteLocalFile(connection.getUri(), local, raw);
                } catch (Exception e) {
                    log.warn("Discog read problem for {}/{}, continuing...", local, raw, e);
                    discogsTired.set(true);
                    return updateAndReadLocalFile(raw, local);
                }
            } catch (Exception e) {
                log.warn("Failed to retrieve data for {}, proceeding", connection, e);
            }
            return Optional.empty();
        });
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

    @SuppressWarnings("SameParameterValue")
    private static <T> Optional<T> withRetry(int retries, Supplier<Optional<T>> fun) {

        return IntStream.range(0, retries).mapToObj(retry -> fun.get()).flatMap(Optional::stream).findFirst();
    }

    private static boolean match(Album album, DiscogConnection meta) {

        return meta.isUp() && meta.getAlbum().equals(album);
    }

    private static Optional<DiscogReleaseDigest> fetchAndWriteLocalFile(
        URI uri,
        Path localDigestPath,
        Path localRawPath
    ) {

        Map<String, ?> rawData = IO.downloadJson(uri, AUTHORIZATION);
        DiscogReleaseDigest digest =
            IO.writeStream(localDigestPath, readRelease(rawData), writeRelease(DiscogReleaseDigest.class));
        IO.writeStream(localRawPath, rawData, writeRelease(Map.class));
        log.info("Updated local digest: {} <== {}", digest.getTitle(), uri);
        return Optional.of(digest);
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

    private static boolean regularFile(Path local) {

        return Files.isRegularFile(local);
    }

    private static Instant modifiedTime(Path local) throws IOException {

        return Files.getLastModifiedTime(local).toInstant();
    }
}
