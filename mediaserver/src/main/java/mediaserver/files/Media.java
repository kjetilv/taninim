package mediaserver.files;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface Media {

    static Media at(Path root) {
        return new LocalMedia(root);
    }

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
}
