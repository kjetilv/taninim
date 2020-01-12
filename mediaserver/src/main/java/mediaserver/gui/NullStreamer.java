package mediaserver.gui;

import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.HttpResponse;
import mediaserver.http.WebPath;
import mediaserver.media.Track;

import java.util.Optional;

public final class NullStreamer extends AbstractStreamer {

    public NullStreamer() {

        super(null, 0);
    }

    @Override
    protected long length(Track track, boolean lossless) {

        return 0L;
    }

    @Override
    protected Optional<ChannelFuture> streamFuture(
        WebPath webPath,
        Track track,
        boolean lossless,
        HttpResponse response
    ) {
        return Optional.empty();
    }
}
