package mediaserver.files;

import mediaserver.hash.AbstractHashable;
import org.gagravarr.flac.FlacFile;
import org.gagravarr.flac.FlacInfo;
import org.gagravarr.flac.FlacTags;

import java.io.File;
import java.io.Serializable;
import java.time.Duration;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class Track extends AbstractHashable
    implements Comparable<Track>, Serializable {

    private final Artist albumArtist;

    private final Artist artist;

    private final String name;

    private final String album;

    private final int trackNo;

    private final Integer part;

    private final String file;

    private static final Pattern PART_TRACK_NAME = Pattern.compile("^(\\d+)-(\\d{2,})\\s+(.*)$");

    private static final Pattern TRACK_NAME = Pattern.compile("^(\\d{2,})\\s+(.*)$");

    private static final Comparator<Track> TRACK_COMPARATOR =
        Comparator.comparing(Track::getTrackNo);

    private static final Comparator<Track> PART_TRACK_COMPARATOR =
        Comparator.comparing(Track::getPart).thenComparing(Track::getTrackNo);

    private final Duration duration;

    private final byte[] signature;

    private static final long serialVersionUID = 3609605456752055320L;

    private final long numberOfSamples;

    private final long fileSize;

    public Track(File file) {

        this.part = part(Objects.requireNonNull(file, "file").getName());
        try (FlacFile flacFile = FlacFile.open(file)) {
            FlacTags tags = flacFile.getTags();
            boolean compilation =
                Optional.ofNullable(tags.getComments("compilation"))
                    .filter(list -> list.size() == 1)
                    .filter(list -> list.contains("1"))
                    .isPresent();
            this.artist = Artist.get(tags.getArtist());
            this.albumArtist = compilation ? null : Optional.ofNullable(tags.getComments("albumartist"))
                .filter(c ->
                    !c.isEmpty())
                .map(c ->
                    c.get(0))
                .map(Artist::get)
                .orElse(this.artist);
            this.name = tags.getTitle();
            this.album = tags.getAlbum();
            this.trackNo = trackNo(file.getName());
            FlacInfo info = flacFile.getInfo();
            this.signature = info.getSignature();
            this.numberOfSamples = info.getNumberOfSamples();
            this.fileSize = file.length();
            this.duration = Duration.ofMillis(numberOfSamples * 1000 / info.getSampleRate());
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to import flac file: " + file, e);
        }
        this.file = file.getPath();
    }

    public Artist getArtist() {

        return artist;
    }

    public Optional<Artist> getOtherArtist() {

        return Objects.equals(artist, albumArtist) ? Optional.empty() : Optional.of(artist);
    }

    public Artist getOtherArtistPresent() {

        return getOtherArtist().orElse(null);
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

    public String getPrettyTrackNo() {

        return part == null ? String.valueOf(trackNo) : part + "-" + trackNo;
    }

    public File getFile() {

        return new File(file);
    }

    public Duration getDuration() {

        return duration;
    }

    public long getSeconds() {

        return duration.toSeconds();
    }

    public String getPrettyDuration() {

        return duration.getSeconds() / 60 + ":" + duration.getSeconds() % 60;
    }

    public boolean sameAlbum(Track track) {

        return track.getArtist().equals(getArtist()) && track.getAlbum().equals(getAlbum());
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public int compareTo(Track track) {

        return TRACK_COMPARATOR.compare(this, track);
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {

        hash(h, signature);
        hash(h, (int) numberOfSamples, (int) fileSize);
    }

    @Override
    protected Object toStringBody() {

        return artist + "/" + album + " " + trackNo + ": " + name;
    }

    private Integer part(String name) {

        if (name == null) {
            return null;
        }
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
}
