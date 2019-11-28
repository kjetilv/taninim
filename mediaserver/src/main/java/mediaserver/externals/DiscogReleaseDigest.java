package mediaserver.externals;

import java.util.List;

public class DiscogReleaseDigest {

    private String artist;

    private String title;

    private String resource_url;

    private String year;

    private String notes;

    private String uri;

    private List<DiscogImage> images;

    private List<DiscogVideo> videos;

    private List<DiscogArtistDigest> artists;

    private List<DiscogArtistDigest> extraartists;

    private List<DiscogTrackDigest> tracklist;

    private List<DiscogSeriesDigest> series;

    @Override
    public String toString() {

        return getClass().getSimpleName() +
            "[" + artist +
            ": " + title +
            " @ " + resource_url +
            "]";
    }

    public String getArtist() {

        return artist;
    }

    public void setArtist(String artist) {

        this.artist = artist;
    }

    public String getTitle() {

        return title;
    }

    public void setTitle(String title) {

        this.title = title;
    }

    public List<DiscogImage> getImages() {

        return images;
    }

    public void setImages(List<DiscogImage> images) {

        this.images = images;
    }

    public List<DiscogVideo> getVideos() {

        return videos;
    }

    public void setVideos(List<DiscogVideo> videos) {

        this.videos = videos;
    }

    public List<DiscogSeriesDigest> getSeries() {

        return series;
    }

    public void setSeries(List<DiscogSeriesDigest> series) {

        this.series = series;
    }

    public String getUri() {

        return uri;
    }

    public void setUri(String uri) {

        this.uri = uri;
    }

    public String getResource_url() {

        return resource_url;
    }

    public void setResource_url(String resource_url) {

        this.resource_url = resource_url;
    }

    public String getYear() {

        return year;
    }

    public void setYear(String year) {

        this.year = year;
    }

    public String getNotes() {

        return notes;
    }

    public void setNotes(String notes) {

        this.notes = notes;
    }

    public List<DiscogArtistDigest> getArtists() {

        return artists;
    }

    public void setArtists(List<DiscogArtistDigest> artists) {

        this.artists = artists;
    }

    public List<DiscogArtistDigest> getExtraartists() {

        return extraartists;
    }

    public void setExtraartists(List<DiscogArtistDigest> extraartists) {

        this.extraartists = extraartists;
    }

    public List<DiscogTrackDigest> getTracklist() {

        return tracklist;
    }

    public void setTracklist(List<DiscogTrackDigest> tracklist) {

        this.tracklist = tracklist;
    }
}
