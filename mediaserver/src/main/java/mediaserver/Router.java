package mediaserver;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

class Router extends SimpleChannelInboundHandler<HttpRequest> {

    private static final Logger log = LoggerFactory.getLogger(Router.class);

    private final Collection<Nettish> nettishes;

    Router(Nettish... nettishes) {
        this.nettishes = Arrays.asList(nettishes);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpRequest req) {
        if (req.decoderResult().isSuccess()) {
            String uri = req.uri();
            if (uri.isEmpty()) {
                Nettish.respond(ctx, FORBIDDEN);
            } else {
                String path = URLDecoder.decode(uri, StandardCharsets.UTF_8);
                handlePath(req, path, ctx);
            }
        } else {
            Nettish.respond(ctx, BAD_REQUEST);
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

    private void handlePath(HttpRequest req, String path, ChannelHandlerContext ctx) {
        nettishes.stream()
            .filter(nettish ->
                path.startsWith(nettish.getPrefix()))
            .findFirst()
            .ifPresentOrElse(
                nettish ->
                    nettish.handle(
                        req, path.substring(nettish.getPrefix().length()), ctx),
                Nettish.reset(ctx));
    }
}
