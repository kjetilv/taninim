package mediaserver.files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface Media {

    Logger log = LoggerFactory.getLogger(Media.class);

    Media subLibrary(CategoryPath categoryPath);

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
        log.info("Scanned: {}", media);
        return media;
    }

    static Media empty() {
        return new LocalMedia(null);
    }
}
