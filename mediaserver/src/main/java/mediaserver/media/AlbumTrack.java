package mediaserver.media;

import java.util.Objects;

import mediaserver.util.AbstractPair;

public final class AlbumTrack extends AbstractPair<Album, Track> {
    
    public AlbumTrack(Album album, Track track) {
        super(
            Objects.requireNonNull(album, "album"),
            Objects.requireNonNull(track, "track"));
    }
    
    public Album getAlbum() {
        return getT1();
    }
    
    public Track getTrack() {
        return getT2();
    }
}
