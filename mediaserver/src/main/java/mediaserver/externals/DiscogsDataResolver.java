package mediaserver.externals;

import mediaserver.media.Album;
import mediaserver.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

public class DiscogsDataResolver {

    private static final Logger log = LoggerFactory.getLogger(DiscogsDataResolver.class);

    private static final String KEY = "jTeXCJgjPPGaXAOQHkqS";

    private static final String SECRET = IO.getProperty("dSec");

    private static final Consumer<BiConsumer<String, String>> AUTHORIZATION = headers ->
        headers.accept("Authorization", "Discogs key=" + KEY + ", secret=" + SECRET);

    private final Path resourcesDirectory;

    private final Collection<DiscogConnection> connections;

    private final Duration refreshTime;

    private final AtomicBoolean discogsTired = new AtomicBoolean();

    public DiscogsDataResolver(
        Path resourcesDirectory,
        Collection<DiscogConnection> connections,
        Duration refreshTime
    ) {

        this.resourcesDirectory = resourcesDirectory;
        this.connections = connections;
        this.refreshTime = refreshTime;
    }

    public Collection<DiscogConnection> getConnections() {

        return List.copyOf(connections);
    }

    public Optional<DiscogReleaseDigest> getDiscogRelease(Album album) {

        return connections.stream()
            .filter(meta ->
                match(album, meta))
            .map(connection -> digest(
                connection,
                pathTo(connection, ".json"),
                pathTo(connection, "-raw.json")))
            .flatMap(Optional::stream)
            .findFirst();
    }

    private Path pathTo(DiscogConnection connection, String suffix) {

        return resourcesDirectory.resolve(
            Paths.get(connection.getType(), connection.getId() + suffix));
    }

    private Optional<DiscogReleaseDigest> digest(DiscogConnection connection, Path local, Path raw) {

        return withRetry(3, retry -> {
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
                Instant.now()
            ).minus(refreshTime).isNegative();
        } catch (IOException e) {
            log.warn("Could not assert age of {}", raw, e);
            return false;
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static <T> Optional<T> withRetry(int retries, IntFunction<Optional<T>> fun) {

        return IntStream.range(0, retries).mapToObj(fun).flatMap(Optional::stream).findFirst();
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
        DiscogReleaseDigest digest = readRelease(rawData);
        IO.writeStream(localDigestPath, digest, writeRelease(DiscogReleaseDigest.class));
        IO.writeStream(localRawPath, rawData, writeRelease(Map.class));
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
