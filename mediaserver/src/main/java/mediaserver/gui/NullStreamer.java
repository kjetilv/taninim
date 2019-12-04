package mediaserver.gui;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import mediaserver.Media;
import mediaserver.externals.FacebookUser;
import mediaserver.files.Track;
import mediaserver.sessions.Sessions;

import java.util.Optional;
import java.util.function.Supplier;

public class NullStreamer extends Streamer {

    public NullStreamer(Supplier<Media> media, Sessions sessions) {

        super(media, sessions);
    }

    @Override
    protected Optional<ChannelFuture> stream(
        HttpRequest req,
        FacebookUser user,
        Track track,
        ChannelHandlerContext ctx,
        HttpResponse res
    ) {

        teapot(req, ctx);
        return Optional.empty();
    }
}
