package mediaserver.gui;

import mediaserver.media.Track;

final class Playable {

    private final Track track;

    private final boolean playable;

    Playable(Track track, boolean playable) {

        this.track = track;
        this.playable = playable;
    }

    public Track getTrack() {

        return track;
    }

    public boolean isPlayable() {

        return playable;
    }
}
