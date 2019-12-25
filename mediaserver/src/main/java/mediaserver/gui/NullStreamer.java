package mediaserver.gui;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponse;
import mediaserver.externals.FacebookUser;
import mediaserver.http.WebPath;
import mediaserver.media.Track;

import java.util.Optional;

public final class NullStreamer extends Streamer {

    public NullStreamer() {

        super(null, null);
    }

    @Override
    protected Optional<ChannelFuture> stream(
        WebPath webPath,
        FacebookUser user,
        Track track,
        ChannelHandlerContext ctx,
        HttpResponse res
    ) {
        return Optional.empty();
    }
}
