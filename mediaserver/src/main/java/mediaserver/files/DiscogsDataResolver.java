package mediaserver.files;

import mediaserver.externals.DiscogReleaseDigest;
import mediaserver.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;

public class DiscogsDataResolver {

    private static final Logger log = LoggerFactory.getLogger(DiscogsDataResolver.class);

    private final Path resourcesDirectory;

    private final Collection<DiscogConnection> connections;

    public DiscogsDataResolver(
        Path resourcesDirectory,
        Collection<DiscogConnection> connections
    ) {

        this.resourcesDirectory = resourcesDirectory;
        this.connections = connections;
    }

    public Optional<DiscogReleaseDigest> getDiscogRelease(Artist artist, Album album) {

        return connections.stream()
            .filter(meta ->
                meta.getAlbum().equals(album) && meta.getArtist().equals(artist))
            .map(connection -> {
                Path localDigestPath =
                    resourcesDirectory.resolve(
                        Paths.get(connection.getType(), connection.getId() + ".json"));
                Path localRawPath =
                    resourcesDirectory.resolve(
                        Paths.get(connection.getType(), connection.getId() + "-raw.json"));
                return digest(connection, localDigestPath, localRawPath);
            })
            .flatMap(Optional::stream)
            .findFirst();
    }

    public Optional<DiscogReleaseDigest> digest(DiscogConnection connection, Path localDigestPath, Path localRawPath) {

        try {
            if (localDigestPath.toFile().isFile() && localRawPath.toFile().isFile()) {
                return readLocalFile(localDigestPath);
            }
            return fetchAndWriteLocalFile(connection.getUri(), localDigestPath, localRawPath);
        } catch (Exception e) {
            log.warn("Failed to retrieve data for {}, proceeding", connection, e);
        }
        return Optional.empty();
    }

    public Optional<DiscogReleaseDigest> fetchAndWriteLocalFile(
        URI uri,
        Path localDigestPath,
        Path localRawPath
    ) {

        Map<String, ?> rawData = IO.readData(uri);
        DiscogReleaseDigest digest = readRelease(rawData);
        IO.writeStream(localDigestPath, digest, writeRelease(DiscogReleaseDigest.class));
        IO.writeStream(localRawPath, rawData, writeRelease(Map.class));
        return Optional.of(digest);
    }

    public Optional<DiscogReleaseDigest> readLocalFile(Path localDigestPath) {

        return Optional.of(
            IO.readStream(localDigestPath, this::readRelease));
    }

    public DiscogReleaseDigest readRelease(InputStream is) {

        try {
            return IO.OM.readerFor(DiscogReleaseDigest.class).readValue(is);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read", e);
        }
    }

    public DiscogReleaseDigest readRelease(Map<String, ?> rawData) {

        try {
            return IO.OM.readerFor(DiscogReleaseDigest.class).readValue(
                IO.OM.writerFor(Map.class).writeValueAsBytes(rawData));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read", e);
        }
    }

    public <T> BiConsumer<T, OutputStream> writeRelease(Class<T> type) {

        return (data, os) -> {
            try {
                IO.OM.writerFor(type).writeValue(os, data);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read", e);
            }
        };
    }
}
