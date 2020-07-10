package mediaserver.stream;

import mediaserver.http.Route;
import mediaserver.media.Track;

public final class NullStreamer extends Streamer {

    public NullStreamer(Route route) {
        super(route, null, () -> {
            throw new UnsupportedOperationException("Not a streamer");
        }, 0);
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
