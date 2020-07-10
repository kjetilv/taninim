package mediaserver.media;

import java.io.Serializable;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
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
import java.util.stream.IntStream;
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

    private final AlbumContext context;

    private final List<Artist> artists;

    private final List<Artist> allArtists;

    private final List<Series> series;

    Album(Artist artist, String name, List<Track> tracks) {
        this(artist, name, tracks, null);
    }

    private Album(Artist artist, String name, List<Track> tracks, AlbumContext context) {
        this(
            artist,
            UNDERSCORE.matcher(name).replaceAll(":"),
            parts(tracks),
            tracks,
            context);
    }

    private Album(
        Artist artist,
        String name,
        Integer parts,
        List<Track> tracks,
        AlbumContext context
    ) {
        this.artist = artist;
        this.context = context == null ? new AlbumContext(this) : context;
        this.name = single(Track::getAlbum, tracks).orElse(name);
        this.parts = parts;
        this.tracks =
            Objects.requireNonNull(tracks, "track").stream().sorted().collect(Collectors.toList());
        this.trackIndex =
            tracks.stream().collect(Collectors.toMap(Track::getUuid, Function.identity()));
        this.artists = this.resolveArtists(false);
        this.allArtists = this.resolveArtists(true);
        this.series = context == null
            ? Collections.emptyList()
            : context.getSeries().stream().map(Series::get).collect(Collectors.toList());
        Collection<Path> paths = tracks.stream().map(Track::getFile).map(Path::getParent).collect(Collectors.toSet());
        if (paths.size() != 1) {
            throw new IllegalStateException("Bad track paths for album " + name + " @ " + paths);
        }
        this.path = paths.iterator().next().toUri();
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

    public AlbumContext getContext() {
        return context;
    }

    public Collection<Artist> getArtists() {
        return artists;
    }

    @DAC
    public Collection<Artist> getAllArtists() {
        return allArtists;
    }

    @DAC
    public boolean isAdditionalArtists() {
        return artists.size() > 1;
    }

    public Collection<Series> getSeries() {
        return series;
    }

    public Integer getParts() {
        return parts;
    }

    public List<Track> getTracks() {
        return tracks;
    }

    public List<Track> getTracksFeaturing(Artist artist) {
        if (artist.equals(getArtist())) {
            return tracks;
        }
        List<TrackContext> trackContexts = context.getTrackContexts();
        return IntStream.range(0, trackContexts.size()).filter(position ->
            Stream.of(
                tracks.get(position).getArtists().stream(),
                tracks.get(position).getOtherArtists().stream(),
                this.context.getTrackContexts().get(position).getCredits().getCredits().stream().map(Credit::getArtist)
            ).flatMap(s -> s)
                .anyMatch(artist::equals))
            .mapToObj(tracks::get)
            .collect(Collectors.toList());
    }

    public boolean features(Artist artist) {
        return getAllArtists().contains(artist);
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

    Album withContext(AlbumContext albumContext) {
        return new Album(artist, name, parts, tracks, albumContext);
    }

    private List<Artist> resolveArtists(boolean all) {
        return Stream.of(
            Stream.of(artist),
            tracks.stream()
                .map(Track::getArtist),
            tracks.stream().map(Track::getOtherArtists).flatMap(Collection::stream),
            context.getCredits().getCredits().stream()
                .filter(Credit::isPerformer)
                .map(Credit::getName)
                .map(Artist::get),
            all
                ? trackArtists()
                : Stream.<Artist>empty())
            .flatMap(s -> s)
            .flatMap(s ->
                s.getCompositeArtists().stream())
            .distinct()
            .collect(Collectors.toList());
    }

    private Stream<Artist> trackArtists() {
        return context.getTrackContexts().stream()
            .map(TrackContext::getCredits)
            .map(Credits::getCredits)
            .flatMap(Collection::stream)
            .filter(Credit::isPerformer)
            .map(Credit::getName)
            .map(Artist::get);
    }

    private static final Comparator<Album> ALBUM_COMPARATOR =
        Comparator.comparing(Album::getArtist).thenComparing(Album::getName);

    private static final long serialVersionUID = 6861992470450497236L;

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
