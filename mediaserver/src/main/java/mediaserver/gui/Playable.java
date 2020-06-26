package mediaserver.gui;

import mediaserver.media.Track;
import mediaserver.util.DAC;

final class Playable
{

    private final Track track;

    private final boolean playable;

    Playable(Track track, boolean playable)
    {

        this.track = track;
        this.playable = playable;
    }

    public Track getTrack()
    {

        return track;
    }

    @DAC
    public boolean isPlayable()
    {

        return playable;
    }
}
