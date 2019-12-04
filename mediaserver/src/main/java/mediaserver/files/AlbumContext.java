package mediaserver.files;

import mediaserver.util.Pairs;

import java.io.Serializable;
import java.net.URI;
import java.time.Year;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AlbumContext implements Serializable {

    private final Album album;

    private final Year year;

    private final URI discogPage;

    private final URI discogCover;

    private final String notes;

    private final Collection<String> series;

    private final Collection<Video> videos;

    private final Credits credits;

    private final List<TrackContext> trackContexts;

    private final List<TrackGroup> trackGroups;

    private static final long serialVersionUID = 873700442732183661L;

    public AlbumContext(Album album) {

        this(album, null, null, null, null, null, null);
    }

    public AlbumContext(
        Album album,
        Year year,
        URI discogPage,
        URI discogCover,
        String notes,
        Collection<String> series,
        Collection<Video> videos
    ) {

        this(
            Objects.requireNonNull(album, "album"),
            year,
            discogPage,
            discogCover,
            notes,
            series,
            videos,
            null,
            null);
    }

    private AlbumContext(
        Album album,
        Year year,
        URI discogPage,
        URI discogCover,
        String notes,
        Collection<String> series,
        Collection<Video> videos,
        Credits credits,
        List<TrackContext> trackContexts
    ) {

        this.album = album;
        this.year = year;
        this.discogPage = discogPage;
        this.discogCover = discogCover;
        this.notes = notes == null || notes.isBlank() ? null : notes.trim();
        this.series = series == null || series.isEmpty() ? Collections.emptyList() : List.copyOf(series);
        this.videos = videos == null || videos.isEmpty() ? Collections.emptyList() : List.copyOf(videos);
        this.credits = credits == null ? new Credits() : credits;
        this.trackContexts = trackContexts == null || trackContexts.isEmpty()
            ? Collections.emptyList()
            : List.copyOf(trackContexts);
        List<Integer> headings = IntStream.range(0, this.trackContexts.size())
            .filter(i ->
                trackContexts.get(i).isHeading())
            .boxed()
            .collect(Collectors.toList());
        if (headings.isEmpty()) {
            this.trackGroups =
                Collections.singletonList(new TrackGroup("Disc", this.trackContexts));
        } else {
            this.trackGroups =
                Pairs.pairs(headings, this.trackContexts.size()).stream()
                    .map(pair ->
                        new TrackGroup(
                            this.trackContexts.get(pair.getT1()).getHeading().orElseGet(() ->
                                "Tracks " + pair.getT1() + "-" + (pair.getT2() - 1)),
                            this.trackContexts.subList(pair.getT1() + 1, pair.getT2())))
                    .collect(Collectors.toList());
        }
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

    public Album getAlbum() {

        return album;
    }

    public Collection<Video> getVideos() {

        return videos;
    }

    public Collection<String> getSeries() {

        return series;
    }

    public List<TrackContext> getTrackContexts() {

        return trackContexts;
    }

    @SuppressWarnings("unused")
    public List<TrackGroup> getTrackGroups() {

        return trackGroups;
    }

    @SuppressWarnings("unused")
    public boolean isAdditionalTrackContext() {

        return !trackContexts.stream().allMatch(trackContext ->
            trackContext.getCredits().getCredits().isEmpty());
    }

    public URI getDiscogCover() {

        return discogCover;
    }

    @SuppressWarnings("unused")
    public URI getDiscogPage() {

        return discogPage;
    }

    public AlbumContext credit(String name, URI uri, String type) {

        return new AlbumContext(
            album,
            year,
            discogPage,
            discogCover,
            notes,
            series,
            videos,
            credits.credit(name, uri, type),
            trackContexts);
    }

    public AlbumContext append(AlbumContext albumContext) {

        if (!album.equals(albumContext.getAlbum())) {

            throw new IllegalArgumentException(
                this + " could not add album context for other album: " + albumContext);
        }

        return new AlbumContext(
            album,
            albumContext.year == null ? year : albumContext.year,
            discogPage,
            discogCover,
            albumContext.notes == null ? notes : albumContext.notes,
            albumContext.series.isEmpty() ? series : albumContext.series,
            albumContext.videos.isEmpty() ? videos : albumContext.videos,
            credits.append(albumContext.getCredits()),
            albumContext.getTrackContexts());
    }

    public AlbumContext withTrackContexts(List<TrackContext> trackContexts) {

        return new AlbumContext(
            album,
            year,
            discogPage,
            discogCover,
            notes,
            series,
            videos,
            credits,
            trackContexts);
    }
}
