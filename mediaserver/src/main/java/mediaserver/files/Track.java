package mediaserver.files;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Track {

    private final String artist;

    private final String name;

    private final String album;

    private final int trackNo;

    private final Integer part;

    private final File file;

    private static final Pattern PART_TRACK_NAME = Pattern.compile("^(\\d+)-(\\d{2,})\\s+.*$");

    private static final Pattern TRACK_NAME = Pattern.compile("^(\\d{2,})\\s+.*$");

    public Track(String artist, String album, String name, File file) {
        this.artist = artist;
        this.name = name;
        this.album = album;
        this.part = part(name);
        this.trackNo = trackNo(name);
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

    public int getTrackNo() {
        return trackNo;
    }

    public File getFile() {
        return file;
    }

    private Integer part(String name) {
        Matcher matcher = PART_TRACK_NAME.matcher(name);
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    private int trackNo(String name) {
        Matcher partMatcher = PART_TRACK_NAME.matcher(name);
        if (partMatcher.matches()) {
            return Integer.parseInt(partMatcher.group(2));
        }
        Matcher matcher = TRACK_NAME.matcher(name);
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(1));
        }
        throw new IllegalArgumentException("Bad track name: " + name);
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
