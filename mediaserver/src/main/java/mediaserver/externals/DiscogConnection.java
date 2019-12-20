package mediaserver.externals;

import mediaserver.media.Album;
import mediaserver.media.Artist;

import java.net.URI;
import java.util.Objects;
import java.util.stream.Stream;

public class DiscogConnection {

    private final Artist artist;

    private final Album album;

    private final URI uri;

    private final String id;

    private final String type;

    public DiscogConnection(Album album, URI uri) {

        this.album = album;
        this.artist = album.getArtist();
        this.uri = uri;

        String uriString = uri.toASCIIString();
        int lastSlash = uriString.lastIndexOf("/");
        this.id = uriString.substring(lastSlash + 1);
        int nextToLastSlash = uriString.substring(0, lastSlash).lastIndexOf("/");
        this.type = uriString.substring(nextToLastSlash + 1, lastSlash);
    }

    public Artist getArtist() {

        return artist;
    }

    public Album getAlbum() {

        return album;
    }

    public URI getUri() {

        return uri;
    }

    public String getId() {

        return id;
    }

    public String getType() {

        return type;
    }

    public boolean isUp() {

        return uri != null && Stream.of("https://api.discogs.com/releases/", "https://api.discogs.com/releases")
            .map(URI::create)
            .noneMatch(uri::equals);
    }

    @Override
    public int hashCode() {

        return Objects.hash(artist, album, uri);
    }

    @Override
    public boolean equals(Object o) {

        return this == o || o instanceof DiscogConnection &&
            Objects.equals(artist, ((DiscogConnection) o).artist) &&
            Objects.equals(album, ((DiscogConnection) o).album) &&
            Objects.equals(uri, ((DiscogConnection) o).uri);
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + artist + ": " + album + "]";
    }
}
