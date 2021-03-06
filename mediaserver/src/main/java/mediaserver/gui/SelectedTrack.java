package mediaserver.gui;

import java.util.Objects;
import javax.annotation.Nullable;

import mediaserver.media.Album;
import mediaserver.media.AlbumTrack;
import mediaserver.media.Track;

final class SelectedTrack {

    private final AlbumTrack albumTrack;

    private final boolean autoplay;

    private final Track previous;

    private final Track next;

    SelectedTrack(AlbumTrack albumTrack, boolean autoplay, Track previous, Track next) {
        this.albumTrack = Objects.requireNonNull(albumTrack, "albumTrack");
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
            (previous == null ? "" : "<< ") +
            albumTrack + " " + (autoplay ? "(autoplay)" : "") +
            (next == null ? "" : " >>") +
            "]";
    }
}
