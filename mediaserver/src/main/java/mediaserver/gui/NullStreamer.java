package mediaserver.gui;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import mediaserver.Media;
import mediaserver.files.Track;
import mediaserver.sessions.Sessions;
import mediaserver.util.IO;

public class NullStreamer extends AbstractStreamer {

    public NullStreamer(IO io, Media media, Sessions sessions) {

        super(io, media, sessions);
    }

    @Override
    protected HttpResponse stream(HttpRequest req, Track track, ChannelHandlerContext ctx) {

        return teapot(req, ctx);
    }
}
