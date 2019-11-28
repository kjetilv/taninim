package mediaserver.gui;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import mediaserver.Media;
import mediaserver.externals.FacebookUser;
import mediaserver.files.Track;
import mediaserver.sessions.Sessions;
import mediaserver.util.IO;

import java.util.Optional;
import java.util.function.Supplier;

public class NullStreamer extends AbstractStreamer {

    public NullStreamer(IO io, Supplier<Media> media, Sessions sessions) {

        super(io, media, sessions);
    }

    @Override
    protected Optional<ChannelFuture> stream(
        HttpRequest req,
        FacebookUser user,
        Track track,
        ChannelHandlerContext ctx
        , HttpResponse res
    ) {
        teapot(req, ctx);
        return Optional.empty();
    }
}
