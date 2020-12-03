package mediaserver.media;

import java.io.Serial;
import java.io.Serializable;
import java.net.URI;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import mediaserver.util.DAC;
import mediaserver.util.Pair;
import mediaserver.util.Pairs;

public final class AlbumContext implements Serializable, Comparable<AlbumContext> {

    private final Album album;

    private final Year year;

    private final Long discogId;

    private final URI discogPage;

    private final URI discogCover;

    private final URI discogArt;

    private final String notes;

    private final Collection<Series> series;

    private final Collection<Video> videos;

    private final Credits credits;

    private final List<TrackContext> trackContexts;

    private final List<TrackGroup> trackGroups;

    private final List<Artist> artists;

    private final List<Artist> allArtists;

    AlbumContext(Album album) {
        this(
            Objects.requireNonNull(album, "album"),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    }

    AlbumContext(
        Album album,
        Year year,
        Long discogId,
        URI discogPage,
        URI discogCover,
        URI discogArt,
        String notes,
        Collection<Series> series,
        Collection<Video> videos
    ) {
        this(
            album,
            year,
            discogId,
            discogPage,
            discogCover,
            discogArt,
            notes,
            series,
            videos,
            null,
            null);
    }

    private AlbumContext(
        Album album,
        Year year,
        Long discogId,
        URI discogPage,
        URI discogCover,
        URI discogArt,
        String notes,
        Collection<Series> series,
        Collection<Video> videos,
        Credits credits,
        List<TrackContext> trackContexts
    ) {
        this.album = album;
        this.year = year;
        this.discogId = discogId;
        this.discogPage = discogPage;
        this.discogCover = discogCover;
        this.discogArt = discogArt;
        this.notes = notes == null || notes.isBlank() ? null : notes.trim();
        this.series = series == null || series.isEmpty() ? Collections.emptyList() : List.copyOf(series);
        this.videos = videos == null || videos.isEmpty() ? Collections.emptyList() : List.copyOf(videos);
        this.credits = credits == null ? new Credits() : credits;
        this.trackContexts = trackContexts == null || trackContexts.isEmpty()
            ? Collections.emptyList()
            : List.copyOf(trackContexts);
        List<Integer> headings = IntStream.range(0, this.trackContexts.size())
            .filter(i ->
                trackContexts != null && trackContexts.size() > i && trackContexts.get(i).isHeading())
            .boxed()
            .collect(Collectors.toList());
        if (headings.isEmpty()) {
            this.trackGroups =
                Collections.singletonList(new TrackGroup("", this.trackContexts));
        } else {
            this.trackGroups =
                Pairs.pairs(headings, this.trackContexts.size()).stream()
                    .filter(pair ->
                        pair.getT1() + 1 < pair.getT2())
                    .map(pair -> {
                        Collection<TrackContext> headingEntries = headingEntries(pair);
                        List<TrackContext> trackEntries =
                            this.trackContexts.subList(pair.getT1() + 1, pair.getT2());
                        String name = name(headingEntries, pair);
                        return new TrackGroup(name, trackEntries);
                    })
                    .collect(Collectors.toList());
        }
        this.artists = resolveArtists(false);
        this.allArtists = resolveArtists(true);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public int compareTo(AlbumContext albumContext) {
        return getAlbum().compareTo(Objects.requireNonNull(albumContext, "albumContext").getAlbum());
    }

    public Credits getCredits() {
        return credits;
    }

    public Year getYear() {
        return year;
    }

    public String getNotes() {
        return notes;
    }

    public Collection<Video> getVideos() {
        return videos;
    }

    public Collection<Series> getSeries() {
        return series;
    }

    public Long getDiscogId() {
        return discogId;
    }

    @DAC
    public List<TrackContext> getTrackContexts() {
        return trackContexts;
    }

    @DAC
    public List<TrackGroup> getTrackGroups() {
        return trackGroups;
    }

    @DAC
    public boolean isAdditionalTrackContext() {
        return !trackContexts.stream().allMatch(trackContext ->
            trackContext.getCredits().getCredits().isEmpty());
    }

    @DAC
    public URI getDiscogCover() {
        return discogCover;
    }

    @DAC
    public URI getDiscogArt() {
        return discogArt;
    }

    @DAC
    public URI getDiscogPage() {
        return discogPage;
    }

    public AlbumContext append(AlbumContext albumContext) {
        if (!album.equals(albumContext.getAlbum())) {
            throw new IllegalArgumentException(
                this + " could not add album context for other album: " + albumContext);
        }
        return new AlbumContext(
            album,
            albumContext.year == null ? year : albumContext.year,
            discogId,
            discogPage,
            discogCover,
            discogArt, albumContext.notes == null ? notes : albumContext.notes,
            albumContext.series.isEmpty() ? series : albumContext.series,
            albumContext.videos.isEmpty() ? videos : albumContext.videos,
            credits.append(albumContext.getCredits()),
            albumContext.getTrackContexts());
    }

    public Album getAlbum() {
        return album;
    }

    public List<Artist> getAllArtists() {
        return allArtists;
    }

    public List<Artist> getArtists() {
        return artists;
    }

    public boolean features(Artist artist) {
        return allArtists.contains(artist);
    }

    public List<Track> getTracksFeaturing(Artist artist) {
        List<Track> tracks = album.getTracks();
        if (artist.equals(album.getArtist())) {
            return tracks;
        }
        List<TrackContext> trackContexts = getTrackContexts();
        return IntStream.range(0, trackContexts.size()).filter(position ->
            Stream.of(
                tracks.get(position).getArtists().stream(),
                tracks.get(position).getOtherArtists().stream(),
                getTrackContexts().get(position).getCredits().getCredits().stream().map(Credit::getArtist))
                .flatMap(s -> s)
                .anyMatch(artist::equals))
            .mapToObj(tracks::get)
            .collect(Collectors.toList());
    }

    AlbumContext credit(String name, URI uri, String type) {
        return new AlbumContext(
            album,
            year,
            discogId,
            discogPage,
            discogCover,
            discogArt,
            notes,
            series,
            videos,
            credits.credit(name, uri, type),
            trackContexts);
    }

    AlbumContext withTrackContexts(List<TrackContext> trackContexts) {
        return new AlbumContext(
            album,
            year,
            discogId,
            discogPage,
            discogCover,
            discogArt,
            notes,
            series,
            videos,
            credits,
            trackContexts);
    }

    private List<Artist> resolveArtists(boolean all) {
        return Stream.<Stream<Artist>>of(
            album.getArtists().stream(),
            getCredits().getCredits().stream()
                .filter(Credit::isPerformer)
                .map(Credit::getName)
                .map(Artist::get),
            all
                ? getTrackContexts().stream()
                .map(TrackContext::getCredits)
                .map(Credits::getCredits)
                .flatMap(Collection::stream)
                .filter(Credit::isPerformer)
                .map(Credit::getName)
                .map(Artist::get)
                : Stream.empty())
            .flatMap(s -> s)
            .map(Artist::getCompositeArtists)
            .flatMap(Collection::stream).collect(Collectors.toList());
    }

    private Collection<TrackContext> headingEntries(Pair<Integer, Integer> pair) {
        List<TrackContext> headingEntries =
            IntStream.range(0, pair.getT1() + 1)
                .map(i ->
                    pair.getT1() - i)
                .takeWhile(i ->
                    this.trackContexts.get(i).isHeading())
                .mapToObj(this.trackContexts::get)
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.reverse(headingEntries);
        return headingEntries;
    }

    @Serial private static final long serialVersionUID = 873700442732183661L;

    private static String name(Collection<TrackContext> headingEntries, Pair<Integer, Integer> pair) {
        if (headingEntries.isEmpty()) {
            return "Tracks " + pair.getT1() + "-" + (pair.getT2() - 1);
        }
        return headingEntries.stream().map(TrackContext::getHeading).flatMap(Optional::stream).collect(
            Collectors.joining(" / "));
    }
}
