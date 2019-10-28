package mediaserver.externals;

import java.net.URI;

public class AlbumMetadata {

    private String discogReleaseId;

    private URI discogRelease;

    private String artist;

    private String title;

    public String getDiscogReleaseId() {

        return discogReleaseId;
    }

    public void setDiscogReleaseId(String discogReleaseId) {

        this.discogReleaseId = discogReleaseId;
    }

    public URI getDiscogRelease() {

        return discogRelease;
    }

    public void setDiscogRelease(URI discogRelease) {

        this.discogRelease = discogRelease;
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
}
