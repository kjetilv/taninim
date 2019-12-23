package mediaserver.externals;

import java.util.List;

public final class DiscogTrackDigest {

    private List<DiscogArtistDigest> extraartists;

    private String title;

    private String position;

    public List<DiscogArtistDigest> getExtraartists() {

        return extraartists;
    }

    public void setExtraartists(List<DiscogArtistDigest> extraartists) {

        this.extraartists = extraartists;
    }

    public String getPosition() {

        return position;
    }

    public void setPosition(String position) {

        this.position = position;
    }

    public String getTitle() {

        return title;
    }

    public void setTitle(String title) {

        this.title = title;
    }

}
