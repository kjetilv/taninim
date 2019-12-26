package mediaserver.gui;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponse;
import mediaserver.http.WebPath;
import mediaserver.media.Track;
import mediaserver.sessions.Session;

import java.util.Optional;

public final class NullStreamer extends AbstractStreamer {

    public NullStreamer() {

        super(null, null);
    }

    @Override
    protected Optional<ChannelFuture> stream(
        WebPath webPath,
        Session user,
        Track track,
        ChannelHandlerContext ctx,
        HttpResponse res
    ) {
        return Optional.empty();
    }
}
