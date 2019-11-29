package mediaserver;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.NotSslRecordException;
import mediaserver.gui.Nettish;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLHandshakeException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static mediaserver.gui.Nettish.respond;

class Router extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger log = LoggerFactory.getLogger(Router.class);

    private static final Collection<String> ROUTED = new ConcurrentSkipListSet<>();

    private final Collection<Nettish> nettishes;

    Router(Nettish... nettishes) {

        this.nettishes = Arrays.asList(nettishes);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) {

        Instant start = Instant.now();
        HttpResponse res;
        try {
            res = response(ctx, req);
        } catch (Exception e) {
            long ms = Duration.between(start, Instant.now()).toMillis();
            throw new IllegalStateException(
                "Failed to respond to " + req.uri() + " (" + ms + " ms)", e);
        }
        long ms = Duration.between(start, Instant.now()).toMillis();
        if (ROUTED.add(req.uri())) {
            log.info("{} -> {}/{} in {}ms",
                req.uri(), res.status(), res.headers().get(HttpHeaderNames.CONTENT_LENGTH), ms);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

        try {
            Optional.of(cause)
                .filter(ignorable(ctx))
                .ifPresentOrElse(
                    ignored ->
                        log.info("{}->{}: {}",
                            get(ctx, Channel::remoteAddress), get(ctx, Channel::localAddress), ignored.toString()),
                    () ->
                        log.error("Caught unknown error {}->{}",
                            get(ctx, Channel::remoteAddress), get(ctx, Channel::localAddress), cause));
        } finally {
            if (ctx.channel().isActive()) {
                respond(ctx, INTERNAL_SERVER_ERROR);
            }
        }
    }

    public Predicate<Throwable> ignorable(ChannelHandlerContext ctx) {

        return e -> {
            if (e instanceof SocketException && e.getMessage().equalsIgnoreCase("Connection reset")) {
                return true;
            }
            boolean testing = ctx.channel().localAddress().toString().contains("0:0:0:0:0:0:0:1:8443");
            for (Throwable t = e; t != null && t.getCause() != t; t = t.getCause()) {
                if (e.getMessage().contains(SSLHandshakeException.class.getName()) &&
                    testing
                ) {
                    return true;
                }
                if (e.getMessage().contains(NotSslRecordException.class.getName()) &&
                    testing) {
                    return true;
                }
            }
            return false;
        };
    }

    private HttpResponse response(ChannelHandlerContext ctx, FullHttpRequest req) {

        String uri = req.uri();
        if (uri.isEmpty()) {
            return respond(ctx, FORBIDDEN);
        }
        String path = URLDecoder.decode(uri, StandardCharsets.UTF_8);
        return handlePath(req, path, ctx);
    }

    private static Supplier<HttpResponse> reset(ChannelHandlerContext ctx) {

        return () ->
            Nettish.redirect(ctx, "/");
    }

    private String get(ChannelHandlerContext ctx, Function<Channel, SocketAddress> remoteAddress) {

        return Optional.of(ctx)
            .map(ChannelHandlerContext::channel)
            .map(remoteAddress)
            .map(Objects::toString)
            .orElse("?");
    }

    private HttpResponse handlePath(FullHttpRequest req, String path, ChannelHandlerContext ctx) {

        if (HttpUtil.is100ContinueExpected(req)) {
            DefaultFullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, CONTINUE);
            ctx.write(res);
            return res;
        }
        try {
            return nettishes.stream()
                .filter(nettish ->
                    nettish.shouldHandle(path))
                .findFirst()
                .map(nettish ->
                    nettish.handle(req, path, ctx))
                .orElseGet(
                    reset(ctx));
        } catch (Exception e) {
            log.error("Failed: {}", req, e);
            return respond(ctx, INTERNAL_SERVER_ERROR);
        }
    }
}
