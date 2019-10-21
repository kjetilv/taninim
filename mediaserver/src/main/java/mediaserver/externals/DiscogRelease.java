package mediaserver.externals;

import java.util.List;

public class DiscogRelease {

    private String artist;

    private String title;

    private String resource_url;

    private List<DiscogArtist> extraartists;

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

    public String getResource_url() {

        return resource_url;
    }

    public void setResource_url(String resource_url) {

        this.resource_url = resource_url;
    }

    public List<DiscogArtist> getExtraartists() {

        return extraartists;
    }

    public void setExtraartists(List<DiscogArtist> extraartists) {

        this.extraartists = extraartists;
    }
}
