package mediaserver.files;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class Album {

    private final String artist;

    private final String name;

    private final Integer parts;

    private final List<Track> tracks;

    private final File file;
    private final Path categoryPath;

    @Override
    public String toString() {
        return getClass().getSimpleName() +
            "[artist=" + artist +
            " name=" + name +
            " parts=" + parts +
            " tracks=" + tracks +
            " file=" + file +
            "]";
    }

    public Album(String artist, String name, List<Track> tracks,
                 File file, Path categoryPath) {
        this(artist, name, null, tracks, file, categoryPath);
    }

    public Album(String artist, String name, Integer parts, List<Track> tracks,
                 File file, Path categoryPath) {
        this.artist = artist.replaceAll("_", ":");
        this.name = name.replaceAll("_", ":");
        this.parts = parts;
        this.tracks = tracks;
        this.file = file;
        this.categoryPath = categoryPath;
    }
}
