package mediaserver.media;

import mediaserver.hash.AbstractHashable;
import mediaserver.util.IO;

import java.io.Serializable;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Album extends AbstractHashable
    implements Comparable<Album>, Serializable {

    private final Artist artist;

    private final String name;

    private final Integer parts;

    private final Integer part;

    private final List<Track> tracks;

    private final CategoryPath categoryPath;

    private final AlbumContext context;

    private static final Comparator<Album> ALBUM_COMPARATOR =
        Comparator.comparing(Album::getCategoryPath)
            .thenComparing(Album::getArtist)
            .thenComparing(Album::getName);

    private static final long serialVersionUID = 6861992470450497236L;

    private final List<Artist> artists;

    private final List<Artist> allArtists;

    private List<Series> series;

    Album(CategoryPath categoryPath, Artist artist, String name, List<Track> tracks) {

        this(categoryPath, artist, name, tracks, null);
    }

    Album(CategoryPath categoryPath, Artist artist, String name, List<Track> tracks, AlbumContext context) {

        this(
            artist,
            name.replaceAll("_", ":"),
            parts(tracks),
            null,
            tracks,
            categoryPath,
            context);
    }

    private Album(
        Artist artist,
        String name,
        Integer parts,
        Integer part,
        List<Track> tracks,
        CategoryPath categoryPath,
        AlbumContext context
    ) {

        this.artist = artist;
        this.context = context == null ? new AlbumContext(this) : context;
        this.name = single(Track::getAlbum, tracks).orElse(name);
        this.parts = parts;
        this.part = part == null ? null : part + 1;
        this.tracks =
            Objects.requireNonNull(tracks, "track").stream().sorted().collect(Collectors.toList());
        this.categoryPath = categoryPath;
        this.artists = this.resolveArtists(false);
        this.allArtists = this.resolveArtists(true);
        this.series = context == null
            ? Collections.emptyList()
            : context.getSeries().stream().map(Series::get).collect(Collectors.toList());
    }

    @SuppressWarnings("unused") // StringTemplate
    public List<Album> getAlbumParts() {

        if (parts == null) {
            return Collections.singletonList(this);
        }
        return IntStream.range(0, parts).mapToObj(part ->
            new Album(
                artist,
                name,
                parts,
                part,
                tracks.stream()
                    .filter(track ->
                        track.getPart() == part + 1)
                    .sorted()
                    .collect(Collectors.toList()),
                categoryPath,
                context
            )).collect(Collectors.toList());
    }

    public Duration getDuration() {

        return getTracks().stream().map(Track::getDuration).reduce(
            Duration.ZERO,
            Duration::plus);
    }

    public String getPrettyDuration() {

        return IO.pretty(getDuration());
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public int compareTo(Album album) {

        return ALBUM_COMPARATOR.compare(this, album);
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

    public Collection<Artist> getAllArtists() {

        return allArtists;
    }

    public boolean isAdditionalArtists() {

        return artists.size() > 1;
    }

    public Collection<Series> getSeries() {

        return series;
    }

    public Integer getParts() {

        return parts;
    }

    public Integer getPart() {

        return part;
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
                this.context.getTrackContexts().get(position).getCredits().getCredits().stream().map(Credit::getArtist))
                .flatMap(s -> s)
                .anyMatch(artist::equals))
            .mapToObj(tracks::get)
            .collect(Collectors.toList());
    }

    public CategoryPath getCategoryPath() {

        return categoryPath;
    }

    public boolean isBy(Artist artist) {

        return artist == null || this.artist.equals(artist);
    }

    public boolean isIn(CategoryPath category) {

        return categoryPath.startsWith(category);
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {

        hash(h, tracks);
    }

    public boolean hasTracksBy(Artist artist) {

        return tracks.stream()
            .anyMatch(track ->
                artist == null ||
                    track.getArtists().stream().anyMatch(artist::equals) ||
                    track.getOtherArtists().stream().anyMatch(artist::equals));
    }

    public Album withContext(AlbumContext albumContext) {

        return new Album(
            artist,
            name,
            parts,
            part,
            tracks,
            categoryPath,
            albumContext);
    }

    public boolean features(Artist artist) {

        return getAllArtists().contains(artist);
    }

    public Optional<Track> getTrack(Integer trackNo) {
        if (trackNo <= tracks.size()) {
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

    @Override
    protected StringBuilder withStringBody(StringBuilder sb) {

        return sb.append(categoryPath.getPath())
            .append(": ").append(artist).append("/").append(name)
            .append(" [").append(tracks.size()).append("]");
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
