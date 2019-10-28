package mediaserver.files;

import mediaserver.externals.DiscogRelease;
import mediaserver.util.IO;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

public class DiscogsData {

    private final Path resourcesDirectory;

    private final Collection<DiscogConnection> connections;

    public DiscogsData(
        Path resourcesDirectory,
        Collection<DiscogConnection> connections
    ) {

        this.resourcesDirectory = resourcesDirectory;
        this.connections = connections;
    }

    public Optional<DiscogRelease> getDiscogRelease(Artist artist, Album album) {

        return connections.stream()
            .filter(meta ->
                meta.getAlbum().equals(album) && meta.getArtist().equals(artist))
            .map(connection -> {
                String id = connection.getId();
                String type = connection.getType();
                URI uri = connection.getUri();
                Path localPath = resourcesDirectory.resolve(Paths.get(type, id + ".json"));
                if (localPath.toFile().isFile()) {
                    return IO.readStream(localPath, this::readRelease);
                }
                DiscogRelease downloaded = IO.tryReadStream(uri, this::readRelease);
                IO.writeStream(localPath, downloaded, this::writeRelease);
                return downloaded;
            })
            .filter(Objects::nonNull)
            .findFirst();
    }

    public DiscogRelease readRelease(InputStream is) {

        try {
            return IO.OM.readerFor(DiscogRelease.class).readValue(is);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read", e);
        }
    }

    public void writeRelease(DiscogRelease discogRelease, OutputStream os) {

        try {
            IO.OM.writerFor(DiscogRelease.class).writeValue(os, discogRelease);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read", e);
        }
    }
}
