package mediaserver.files;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface Media {

    Media subLibrary(CategoryPath categoryPath);

    Optional<Track> getSong(UUID uuid);

    default boolean isSubCategories() {
        return !getTopCategories().isEmpty();
    }

    CategoryPath getCategoryPath();

    Collection<CategoryPath> getTopCategories();

    Collection<CategoryPath> getCategories();

    default Collection<String> getAlbumArtists() {
        return getAlbumArtists(false);
    }

    Collection<String> getAlbumArtists(boolean recurse);

    default Collection<String> getArtists() {
        return getArtists(false);
    }

    Collection<String> getArtists(boolean recurse);

    default Collection<Album> getAlbums() {
        return getAlbums(false);
    }

    Collection<Album> getAlbums(boolean recurse);

    default Collection<Track> getSongs() {
        return getSongs(false);
    }

    Collection<Track> getTracksBy(String artist);

    Collection<Track> getSongs(boolean recurse);

    Optional<Album> getAlbum(UUID id);
}
