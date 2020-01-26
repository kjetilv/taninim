package mediaserver.stream;

import mediaserver.media.Track;
import mediaserver.toolkit.Chunk;

public final class NullStreamer extends Streamer {

    public NullStreamer() {

        super(null, null, 0);
    }

    @Override
    protected Object content(Track track, Chunk chunk, boolean lossless) {

        return null;
    }

    @Override
    protected long trackLength(Track track, boolean lossless) {

        return 0L;
    }
}
