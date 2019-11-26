package mediaserver.files;

import java.io.Serializable;
import java.net.URI;
import java.time.Year;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AlbumContext implements Serializable {

    private final Album album;

    private final Year year;

    private final URI discogPage;

    private final URI discogCover;

    private final String notes;

    private final Collection<String> series;

    private final Credits credits;

    private final List<TrackContext> trackContexts;

    private static final long serialVersionUID = 873700442732183661L;

    public AlbumContext(Album album) {

        this(album, null, null, null, null, null);
    }

    public AlbumContext(Album album, Year year, URI discogPage, URI discogCover, String notes, Collection<String> series) {

        this(
            Objects.requireNonNull(album, "album"),
            year,
            discogPage,
            discogCover,
            notes,
            series,
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
        Credits credits,
        List<TrackContext> trackContexts
    ) {

        this.album = album;
        this.year = year;
        this.discogPage = discogPage;
        this.discogCover = discogCover;
        this.notes = notes == null || notes.isBlank() ? null : notes.trim();
        this.series = series == null || series.isEmpty() ? Collections.emptyList() : List.copyOf(series);
        this.credits = credits == null ? new Credits() : credits;
        this.trackContexts = trackContexts == null || trackContexts.isEmpty()
            ? Collections.emptyList()
            : List.copyOf(trackContexts);
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

    public Collection<String> getSeries() {

        return series;
    }

    public List<TrackContext> getTrackContexts() {

        return trackContexts;
    }

    public boolean isAdditionalTrackContext() {

        return !trackContexts.stream().allMatch(trackContext ->
            trackContext.getCredits().getCredits().isEmpty());
    }

    public URI getDiscogCover() {

        return discogCover;
    }

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
            credits,
            trackContexts);
    }
}
