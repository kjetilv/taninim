package mediaserver.gui;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import mediaserver.Media;
import mediaserver.files.Track;
import mediaserver.util.IO;

public class NullStreamer extends AbstractStreamer {

    public NullStreamer(IO io, Media media) {

        super(io, media);
    }

    @Override
    protected HttpResponse stream(HttpRequest req, Track track, ChannelHandlerContext ctx) {

        return respond(ctx, req.uri(), HttpResponseStatus.valueOf(418, "I'm a teapot"));
    }
}
