package mediaserver.gui;

import mediaserver.media.Track;
import mediaserver.toolkit.Chunk;

public final class NullStreamer extends AbstractStreamer {

    public NullStreamer() {

        super(null, 0);
    }

    @Override
    protected Object content(Track track, Chunk chunk, boolean lossless) {

        return null;
    }

    @Override
    protected long length(Track track, boolean lossless) {

        return 0L;
    }
}
