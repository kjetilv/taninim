package mediaserver.gui;

import mediaserver.media.Track;

public final class Selected {

    private final Track track;

    private final boolean autoplay;

    private final Track previous;

    private final Track next;

    public Selected(Track track, boolean autoplay, Track previous, Track next) {

        this.track = track;
        this.autoplay = autoplay;
        this.previous = previous;
        this.next = next;
    }

    public Track getTrack() {

        return track;
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
}
