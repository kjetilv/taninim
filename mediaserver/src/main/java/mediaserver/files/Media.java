package mediaserver.files;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import mediaserver.externals.DiscogRelease;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface Media {

    Logger log = LoggerFactory.getLogger(Media.class);

    Media subLibrary(CategoryPath categoryPath);

    Media addAlbumContext(UUID albumId, AlbumContext albumContext);

    Optional<Track> getTrack(UUID uuid);

    default boolean isSubCategories() {

        return !getTopCategories().isEmpty();
    }

    CategoryPath getCategoryPath();

    Collection<CategoryPath> getTopCategories();

    Collection<CategoryPath> getCategories();

    default Collection<Artist> getAlbumArtists() {

        return getAlbumArtists(false);
    }

    Collection<Artist> getAlbumArtists(boolean recurse);

    default Collection<Artist> getArtists() {

        return getArtists(false);
    }

    Collection<Artist> getArtists(boolean recurse);

    default Collection<Album> allAlbums() {

        return getAlbums(true);
    }

    default Collection<Album> getAlbums() {

        return getAlbums(false);
    }

    Collection<Album> getAlbums(boolean recurse);

    default Collection<Track> getTracks() {

        return getTracks(false);
    }

    default Collection<Track> getAllTracks() {

        return getTracks(true);
    }

    Collection<Track> getTracksBy(Artist artist);

    Collection<Track> getTracks(boolean recurse);

    Optional<Album> getAlbum(UUID id);

    Optional<Artist> getArtist(UUID id);

    static Media local(String file) {

        Path mediaPath = new File(file).toPath();
        return local(mediaPath);
    }

    static Media local(Path mediaPath) {

        log.info("Scanning from {}", mediaPath);
        Media media = new LocalMedia(mediaPath);
        media.allAlbums().stream().reduce(
            media,
            (med, alb) -> {
                String name = "meta/album." + alb.getUuid() + "/data.json";
                try (InputStream res = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                    name)) {
                    if (res == null) {
                        return med;
                    }
                    DiscogRelease data = new ObjectMapper()
                        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                        .readerFor(DiscogRelease.class)
                        .readValue(res);
                    AlbumContext context = data.getExtraartists().stream().reduce(
                        new AlbumContext(alb),
                        (albumContext1, discogArtist) ->
                            albumContext1.credit(
                                discogArtist.getName(),
                                discogArtist.getUri(),
                                discogArtist.getRole()),
                        (albumContext1, albumContext2) -> {
                            throw new IllegalStateException("No combine");
                        });
                    return media.addAlbumContext(alb.getUuid(), context);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to read context for " + alb, e);
                }
            },
            (media1, media2) -> {
                throw new IllegalStateException("NO combine");
            }
        );
        log.info("Scanned: {}", media);
        return media;
    }

    static Media empty() {

        return new LocalMedia(null);
    }
}
