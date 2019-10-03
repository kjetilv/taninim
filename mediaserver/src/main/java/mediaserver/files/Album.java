package mediaserver.files;

import java.io.File;
import java.util.*;

public class Album implements Comparable<Album> {

    private final String artist;

    private final String name;

    private final Integer parts;

    private final List<Track> tracks;

    private final File file;

    private final CategoryPath categoryPath;

    private final UUID uuid = UUID.randomUUID();

    private static final Comparator<Album> ALBUM_COMPARATOR =
        Comparator.comparing(Album::getCategoryPath)
            .thenComparing(Album::getArtist)
            .thenComparing(Album::getName);

    public Album(String artist, String name, List<Track> tracks, File file, CategoryPath categoryPath) {
        this.artist = artist.replaceAll("_", ":");
        this.name = name.replaceAll("_", ":");
        this.parts = parts(tracks);
        this.tracks = tracks;
        this.file = file;
        this.categoryPath = categoryPath;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public int compareTo(Album album) {
        return ALBUM_COMPARATOR.compare(this, album);
    }

    public String getArtist() {
        return artist;
    }

    public String getName() {
        return name;
    }

    public Integer getParts() {
        return parts;
    }

    public List<Track> getTracks() {
        return tracks;
    }

    public File getFile() {
        return file;
    }

    public CategoryPath getCategoryPath() {
        return categoryPath;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String print() {
        return categoryPath + "/ " + getArtist() + ": " + getName() + " [" + tracks.size() + "]";
    }

    private static Integer parts(List<Track> tracks) {
        OptionalInt maxPart = tracks.stream()
            .map(Track::getPart)
            .filter(Objects::nonNull)
            .mapToInt(Integer::intValue)
            .max();
        return maxPart.isPresent() ? maxPart.getAsInt() : null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
            "[artist=" + artist +
            " name=" + name +
            " parts=" + parts +
            " tracks=" + tracks +
            " file=" + file +
            " categoryPath=" + categoryPath +
            "]";
    }
}
