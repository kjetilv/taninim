package mediaserver;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

class Router extends SimpleChannelInboundHandler<HttpRequest> {

    private static final Logger log = LoggerFactory.getLogger(Router.class);

    private final API api;

    private final Streamer streamer;

    private static final String AUDIO = "/audio/";

    private static final String API = "/api/";

    Router(API api, Streamer streamer) {
        this.api = api;
        this.streamer = streamer;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpRequest req) {
        if (req.decoderResult().isSuccess()) {
            String uri = req.uri();
            if (!uri.isEmpty()) {
                String path = URLDecoder.decode(uri, StandardCharsets.UTF_8);
                handlePath(req, path, ctx);
            } else {
                Nettish.respond(ctx, FORBIDDEN);
            }
        } else {
            Nettish.respond(ctx, BAD_REQUEST);
        }
    }

    private void handlePath(HttpRequest req, String path, ChannelHandlerContext ctx) {
        if (path.startsWith(AUDIO)) {
            streamer.stream(req, path.substring("/audio".length()), ctx);
        } else if (path.startsWith(API)) {
            api.handle(req, path.substring("/api".length()), ctx);
        } else {
            Nettish.respond(ctx, NOT_FOUND);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        try {
            log.error("Caught error", cause);
        } finally {
            if (ctx.channel().isActive()) {
                Nettish.respond(ctx, INTERNAL_SERVER_ERROR);
            }
        }
    }

}
