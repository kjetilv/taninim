package mediaserver.media;

import java.io.Serial;
import java.io.Serializable;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import mediaserver.hash.AbstractHashable;
import mediaserver.util.DAC;
import mediaserver.util.Print;

public final class Album extends AbstractHashable
    implements Comparable<Album>, Serializable {

    private final URI path;

    private final Artist artist;

    private final String name;

    private final Integer parts;

    private final List<Track> tracks;

    private final Map<UUID, Track> trackIndex;

    private final List<Artist> artists;

    Album(Artist artist, String name, List<Track> tracks) {
        this(
            artist,
            UNDERSCORE.matcher(name).replaceAll(":"),
            parts(tracks),
            tracks);
    }

    private Album(Artist artist, String name, Integer parts, List<Track> tracks) {
        this.artist = artist;
        this.name = single(Track::getAlbum, tracks).orElse(name);
        this.parts = parts;
        this.tracks =
            Objects.requireNonNull(tracks, "track").stream().sorted().collect(Collectors.toList());
        this.trackIndex =
            tracks.stream().collect(Collectors.toMap(Track::getUuid, Function.identity()));
        this.artists = this.resolveArtists();
        Collection<Path> paths = tracks.stream().map(Track::getFile).map(Path::getParent).collect(Collectors.toSet());
        if (paths.size() != 1) {
            throw new IllegalStateException("Bad track paths for album " + name + " @ " + paths);
        }
        this.path = paths.iterator().next().toUri();

        if (this.tracks.isEmpty()) {
            throw new IllegalStateException("Empty album: " + this);
        }
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public int compareTo(Album album) {
        return ALBUM_COMPARATOR.compare(this, album);
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hash(h, tracks);
    }

    @Override
    protected StringBuilder withStringBody(StringBuilder sb) {
        return sb
            .append(artist).append("/").append(name)
            .append(" [").append(tracks.size()).append("]");
    }

    public Track getStartTrack() {
        return tracks.get(0);
    }

    public Map<String, Collection<Track>> getTracksByPart() {
        List<Integer> parts = tracks.stream()
            .map(Track::getPart)
            .filter(Objects::nonNull)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
        if (parts.isEmpty()) {
            return Map.of("", tracks);
        }
        return parts.stream().collect(Collectors.toMap(
            part ->
                part < 0 ? "" : String.valueOf(part),
            part ->
                tracks.stream().filter(track ->
                    part.equals(track.getPart())).collect(Collectors.toList()),
            (o1, o2) -> {
                throw new IllegalStateException("Failed combine: " + o1 + " / " + o2);
            },
            LinkedHashMap::new
        ));
    }

    public Duration getDuration() {
        return getTracks().stream().map(Track::getDuration).reduce(Duration.ZERO, Duration::plus);
    }

    @DAC
    public String getPrettyDuration() {
        return Print.prettyTrackTime(getDuration());
    }

    public Path getPath() {
        return Paths.get(path);
    }

    public Artist getArtist() {
        return artist;
    }

    public String getName() {
        return name;
    }

    public Collection<Artist> getArtists() {
        return artists;
    }

    @DAC
    public boolean isAdditionalArtists() {
        return artists.size() > 1;
    }

    public Integer getParts() {
        return parts;
    }

    public List<Track> getTracks() {
        return tracks;
    }

    public Optional<Track> getTrack(UUID trackNo) {
        return Optional.ofNullable(trackIndex.get(trackNo));
    }

    public Optional<Track> getTrack(Integer trackNo) {
        if (trackNo != null && trackNo <= tracks.size()) {
            return tracks.stream()
                .filter(track ->
                    trackNo.equals(track.getTrackNo()))
                .findFirst();
        }
        return Optional.empty();
    }

    public Optional<Track> getTrack(Integer disc, Integer trackNo) {
        if (disc <= parts) {
            return tracks.stream()
                .filter(track ->
                    track.getPart().equals(disc) && trackNo.equals(track.getTrackNo()))
                .findFirst();
        }
        return Optional.empty();
    }

    boolean isBy(Artist artist) {
        return artist == null || this.artist.equals(artist);
    }

    boolean hasTracksBy(Artist artist) {
        return tracks.stream()
            .anyMatch(track ->
                artist == null ||
                    track.getArtists().stream().anyMatch(artist::equals) ||
                    track.getOtherArtists().stream().anyMatch(artist::equals));
    }

    private List<Artist> resolveArtists() {
        Stream<Artist> artistStream = Stream.of(
            Stream.of(artist),
            tracks.stream()
                .map(Track::getArtist),
            tracks.stream().map(Track::getOtherArtists).flatMap(Collection::stream))
            .flatMap(s -> s);
        return artistStream
            .flatMap(s ->
                s.getCompositeArtists().stream())
            .distinct()
            .collect(Collectors.toList());
    }

    private static final Comparator<Album> ALBUM_COMPARATOR =
        Comparator.comparing(Album::getArtist).thenComparing(Album::getName);

    @Serial private static final long serialVersionUID = 6861992470450497236L;

    private static final Pattern UNDERSCORE = Pattern.compile("_");

    private static Optional<String> single(Function<Track, String> getAlbum, List<Track> tracks) {
        Collection<String> names = tracks.stream().map(getAlbum).collect(Collectors.toSet());
        return names.size() == 1 ? names.stream().findFirst() : Optional.empty();
    }

    private static Integer parts(List<Track> tracks) {
        OptionalInt maxPart = tracks.stream()
            .map(Track::getPart)
            .filter(Objects::nonNull)
            .mapToInt(Integer::intValue)
            .max();
        return maxPart.isPresent() ? maxPart.getAsInt() : null;
    }
}
