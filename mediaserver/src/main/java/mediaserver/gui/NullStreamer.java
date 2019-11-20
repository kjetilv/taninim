package mediaserver.gui;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import mediaserver.Media;
import mediaserver.files.Track;
import mediaserver.sessions.Sessions;
import mediaserver.util.IO;

import java.util.function.Supplier;

public class NullStreamer extends AbstractStreamer {

    public NullStreamer(IO io, Supplier<Media> media, Sessions sessions) {

        super(io, media, sessions);
    }

    @Override
    protected ChannelFuture stream(HttpRequest req, Track track, ChannelHandlerContext ctx, HttpResponse res) {
        teapot(req, ctx);
        return null;
    }
}
