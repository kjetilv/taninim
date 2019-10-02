package mediaserver.files;

import java.io.File;

public class Track {

    private final String artist;

    private final String name;

    private final String album;

    private final Integer part;

    private final File file;

    public Track(String artist, String album, String name, File file) {
        this(artist, album, name, null, file);
    }

    public Track(String artist, String name, String album, Integer part, File file) {
        this.artist = artist;
        this.name = name;
        this.album = album;
        this.part = part;
        this.file = file;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public String getName() {
        return name;
    }

    public Integer getPart() {
        return part;
    }

    public File getFile() {
        return file;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
            "[artist=" + artist +
            " name=" + name +
            " album=" + album +
            " part=" + part +
            " file=" + file +
            "]";
    }
}
