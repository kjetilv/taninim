package mediaserver.media;

import mediaserver.util.AbstractPair;
import mediaserver.util.Pair;

public class AlbumTrack extends AbstractPair<Album, Track> {

    public AlbumTrack(Album album, Track track) {

        super(album, track);
    }

    public Album getAlbum() {
        return getT1();
    }

    public Track getTrack() {
        return getT2();
    }
}
