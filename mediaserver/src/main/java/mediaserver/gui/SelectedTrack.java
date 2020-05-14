package mediaserver.gui;

import mediaserver.media.Album;
import mediaserver.media.AlbumTrack;
import mediaserver.media.Track;

final class SelectedTrack {

    private final AlbumTrack albumTrack;

    private final boolean autoplay;

    private final Track previous;

    private final Track next;

    SelectedTrack(AlbumTrack albumTrack, boolean autoplay, Track previous, Track next) {

        this.albumTrack = albumTrack;
        this.autoplay = autoplay;
        this.previous = previous;
        this.next = next;
    }

    public Album getAlbum() {

        return albumTrack.getAlbum();
    }

    public Track getTrack() {

        return albumTrack.getTrack();
    }

    public boolean isAutoplay() {

        return autoplay;
    }

    public Track getPrevious() {

        return previous;
    }

    public Track getNext() {

        return next;
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" +
            albumTrack +
            " autoplay=" + autoplay +
            (next == null ? "" : ", next") +
            (previous == null ? "" : ", prev") +
            "]";
    }
}
