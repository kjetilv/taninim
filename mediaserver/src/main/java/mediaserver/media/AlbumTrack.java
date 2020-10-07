package mediaserver.media;

import java.util.Objects;

import mediaserver.util.AbstractPair;

public final class AlbumTrack extends AbstractPair<AlbumContext, Track> {

    public AlbumTrack(AlbumContext albumContext, Track track) {
        super(
            Objects.requireNonNull(albumContext, "album"),
            Objects.requireNonNull(track, "track"));
    }

    public AlbumContext getAlbumContext() {
        return getT1();
    }

    public Album getAlbum() {
        return getT1().getAlbum();
    }

    public Track getTrack() {
        return getT2();
    }
}
