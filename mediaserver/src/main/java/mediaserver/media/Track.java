package mediaserver.media;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mediaserver.hash.AbstractHashable;
import mediaserver.util.DAC;
import mediaserver.util.Print;
import org.gagravarr.flac.FlacFile;
import org.gagravarr.flac.FlacInfo;
import org.gagravarr.flac.FlacTags;

public final class Track extends AbstractHashable
    implements Comparable<Track>, Serializable {

    private final Artist artist;

    private final Collection<Artist> artists;

    private final Collection<Artist> albumArtists;

    private final String name;

    private final String album;

    private final int trackNo;

    private final Integer part;

    private final URI file;

    private final Duration duration;

    private final byte[] signature;

    private final long numberOfSamples;

    private final long fileSize;

    private final URI compressedFile;

    private final long compressedSize;

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
            this.artists = this.artist.getCompositeArtists();
            this.albumArtists = getAlbumArtist(tags, compilation)
                .orElse(this.artist)
                .getCompositeArtists();
            this.name = tags.getTitle();
            this.album = tags.getAlbum();
            this.trackNo = trackNo(file.getName());
            FlacInfo info = flacFile.getInfo();
            this.signature = info.getSignature();
            this.numberOfSamples = info.getNumberOfSamples();
            this.duration = Duration.ofMillis(numberOfSamples * 1000 / info.getSampleRate());
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to import flac file: " + file, e);
        }
        try {
            Path path = file.toPath();
            this.file = path.toUri();
            this.fileSize = Files.size(path);
            Path compressedPath = new File(
                DOT_FLAC.matcher(
                    FLAC.matcher(file.getCanonicalPath())
                        .replaceAll("M4A"))
                    .replaceAll(".m4a")
            ).toPath();
            if (!compressedPath.toFile().exists()) {
                throw new IllegalStateException(this + " has no compressed version @ " + compressedPath);
            }
            this.compressedFile = compressedPath.toUri();
            compressedSize = Files.size(compressedPath);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed file: " + file, e);
        }
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
    protected StringBuilder withStringBody(StringBuilder sb) {
        return sb.append(artist)
            .append("/").append(album)
            .append(" ").append(trackNo)
            .append(". ").append(name);
    }

    public Artist getArtist() {
        return artist;
    }

    public Collection<Artist> getArtists() {
        return artists;
    }

    @DAC
    public Collection<Artist> getOtherArtists() {
        return albumArtists.containsAll(artists) ? Collections.emptyList() : artists;
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

    @DAC
    public String getPrettyTrackNo() {
        return part == null ? String.valueOf(trackNo) : part + "-" + trackNo;
    }

    public Path getFile() {
        return Paths.get(file);
    }

    public long getFileSize() {
        return fileSize;
    }

    public Path getCompressedFile() {
        return Paths.get(compressedFile);
    }

    public long getCompressedSize() {
        return compressedSize;
    }

    public Duration getDuration() {
        return duration;
    }

    public long getSeconds() {
        return duration.toSeconds();
    }

    @DAC
    public String getPrettyDuration() {
        return Print.prettyTrackTime(this.duration);
    }

    @DAC
    public String getPrettyCompressedSize() {
        return Print.bytes(compressedSize);
    }

    @DAC
    public String getPrettyFileSize() {
        return Print.bytes(fileSize);
    }

    private static final Pattern PART_TRACK_NAME = Pattern.compile("^(\\d+)-(\\d{2,})\\s+(.*)$");

    private static final Pattern TRACK_NAME = Pattern.compile("^(\\d{2,})\\s+(.*)$");

    private static final Comparator<Track> TRACK_COMPARATOR =
        Comparator.comparing(Track::getTrackNo);

    private static final long serialVersionUID = 3609605456752055320L;

    private static final Pattern FLAC = Pattern.compile("FLAC");

    private static final Pattern DOT_FLAC = Pattern.compile(".flac");

    private static Optional<Artist> getAlbumArtist(FlacTags tags, boolean compilation) {
        return compilation ? Optional.empty() : Optional.ofNullable(tags.getComments("albumartist"))
            .filter(c ->
                !c.isEmpty())
            .map(c ->
                c.get(0))
            .map(Artist::get);
    }

    private static Integer part(String name) {
        if (name == null) {
            return null;
        }
        Matcher matcher = PART_TRACK_NAME.matcher(name);
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    private static int trackNo(String fileName) {
        return Integer.parseInt(pick(fileName));
    }

    private static String pick(String name) {
        Matcher partMatcher = PART_TRACK_NAME.matcher(name);
        if (partMatcher.matches()) {
            return partMatcher.group(2);
        }
        Matcher matcher = TRACK_NAME.matcher(name);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("Bad track name: " + name);
    }
}
