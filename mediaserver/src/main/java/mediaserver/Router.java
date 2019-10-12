package mediaserver;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION;
import static io.netty.handler.codec.http.HttpResponseStatus.*;

class Router extends SimpleChannelInboundHandler<HttpRequest> {

    private static final Logger log = LoggerFactory.getLogger(Router.class);

    private final Collection<Nettish> nettishes;

    Router(Nettish... nettishes) {
        this.nettishes = Arrays.asList(nettishes);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpRequest req) {
        HttpResponse res = response(ctx, req);
        log.debug("Responded to {}: {}", req.uri(), res.status());
    }

    private HttpResponse response(ChannelHandlerContext ctx, HttpRequest req) {
        if (req.decoderResult().isSuccess()) {
            String uri = req.uri();
            if (uri.isEmpty()) {
                return Nettish.respond(ctx, FORBIDDEN);
            }
            String path = URLDecoder.decode(uri, StandardCharsets.UTF_8);
            return handlePath(req, path, ctx);
        }
        return Nettish.respond(ctx, BAD_REQUEST);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        try {
            Optional.of(cause)
                .filter(SocketException.class::isInstance)
                .map(Throwable::getMessage)
                .map("Connection reset"::equals)
                .ifPresentOrElse(ignored -> {
                    log.info("Connection {}->{} reset",
                        get(ctx, Channel::remoteAddress), get(ctx, Channel::localAddress));
                }, () -> {
                    log.error("Caught error {}->{}",
                        get(ctx, Channel::remoteAddress), get(ctx, Channel::localAddress), cause);
                });
        } finally {
            if (ctx.channel().isActive()) {
                Nettish.respond(ctx, INTERNAL_SERVER_ERROR);
            }
        }
    }

    private static Supplier<HttpResponse> reset(ChannelHandlerContext ctx) {
        return () ->
            Nettish.respond(ctx, new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                FOUND,
                Unpooled.buffer(0),
                new DefaultHttpHeaders()
                    .set(LOCATION, "/"),
                EmptyHttpHeaders.INSTANCE));
    }

    private String get(ChannelHandlerContext ctx, Function<Channel, SocketAddress> remoteAddress) {
        return Optional.of(ctx).map(ChannelHandlerContext::channel).map(remoteAddress).map(Objects::toString).orElse("?");
    }

    private HttpResponse handlePath(HttpRequest req, String path, ChannelHandlerContext ctx) {
        return nettishes.stream()
            .filter(nettish ->
                path.startsWith(nettish.getPrefix()))
            .findFirst()
            .map(
                nettish ->
                    nettish.handle(req, path.substring(nettish.getPrefix().length()), ctx)
            ).orElseGet(
                reset(ctx));
    }
}
