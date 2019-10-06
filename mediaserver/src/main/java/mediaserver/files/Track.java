package mediaserver.files;

import java.io.File;
import java.util.Comparator;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Track implements Comparable<Track> {

    private final String artist;

    private final String name;

    private final String album;

    private final int trackNo;

    private final Integer part;

    private final File file;

    private final UUID uuid = UUID.randomUUID();

    private static final Pattern PART_TRACK_NAME = Pattern.compile("^(\\d+)-(\\d{2,})\\s+(.*)$");

    private static final Pattern TRACK_NAME = Pattern.compile("^(\\d{2,})\\s+(.*)$");

    private static final Comparator<Track> PART_TRACK_COMPARATOR =
        Comparator.comparing(Track::getPart).thenComparing(Track::getTrackNo);

    private static final Comparator<Track> TRACK_COMPARATOR =
        Comparator.comparing(Track::getTrackNo);

    public Track(String artist, String album, String name, File file) {
        this.artist = artist;
        this.name = trackName(name);
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

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public int compareTo(Track track) {
        if (track.getAlbum().equals(getAlbum()) && track.getArtist().equals(getArtist())) {
            return (part == null ? TRACK_COMPARATOR : PART_TRACK_COMPARATOR).compare(this, track);
        }
        return 0;
    }

    private Integer part(String name) {
        Matcher matcher = PART_TRACK_NAME.matcher(name);
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    private int trackNo(String fileName) {
        return Integer.parseInt(pick(fileName, 2, 1));
    }

    private String trackName(String fileName) {
        String name = pick(fileName, 3, 2);
        return name.endsWith(".flac") ? name.substring(0, name.length() - ".flac".length()) : name;
    }

    private String pick(String name, int partIndex, int noPartIndex) {
        Matcher partMatcher = PART_TRACK_NAME.matcher(name);
        if (partMatcher.matches()) {
            return partMatcher.group(partIndex);
        }
        Matcher matcher = TRACK_NAME.matcher(name);
        if (matcher.matches()) {
            return matcher.group(noPartIndex);
        }
        throw new IllegalArgumentException("Bad track name: " + name);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
            "[" + artist + "/" + album + " " + trackNo + ": " + name + "]";
    }
}
