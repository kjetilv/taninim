package mediaserver;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.NotSslRecordException;
import mediaserver.http.Nettish;
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

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static mediaserver.http.Nettish.respond;

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
        HttpResponse response = response(ctx, req);
        log(req, response, duration(start));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

        try {
            ignoredException(ctx, cause).ifPresentOrElse(
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

    private void log(FullHttpRequest req, HttpResponse response, long ms) {

        if (ROUTED.add(req.uri())) {
            log.info("{} -> {}/{} in {}ms",
                req.uri(), response.status(), response.headers().get(HttpHeaderNames.CONTENT_LENGTH), ms);
        }
    }

    private long duration(Instant start) {

        return Duration.between(start, Instant.now()).toMillis();
    }

    private Optional<Throwable> ignoredException(ChannelHandlerContext ctx, Throwable cause) {

        return Optional.of(cause)
            .filter(e -> {
                if (e instanceof SocketException && e.getMessage().equalsIgnoreCase("Connection reset")) {
                    return true;
                }
                boolean testing = ctx.channel().localAddress().toString().contains("0:0:0:0:0:0:0:1:8443");
                for (Throwable t = e; t != null && t.getCause() != t; t = t.getCause()) {
                    if ((is(SSLHandshakeException.class, e) || is(NotSslRecordException.class, e)) && testing) {
                        return true;
                    }
                }
                return false;
            });
    }

    private boolean is(Class<?> sslHandshakeExceptionClass, Throwable e) {

        return e.getMessage().contains(sslHandshakeExceptionClass.getName());
    }

    private HttpResponse response(ChannelHandlerContext ctx, FullHttpRequest req) {
        String uri = req.uri();
        if (uri.isBlank()) {
            return respond(ctx, BAD_REQUEST);
        }
        String path = URLDecoder.decode(uri, StandardCharsets.UTF_8);
        return handlePath(req, path, ctx).orElseGet(() -> respond(ctx, BAD_REQUEST));
    }

    private String get(ChannelHandlerContext ctx, Function<Channel, SocketAddress> remoteAddress) {

        return Optional.of(ctx)
            .map(ChannelHandlerContext::channel)
            .map(remoteAddress)
            .map(Objects::toString)
            .orElse("?");
    }

    private Optional<HttpResponse> handlePath(FullHttpRequest req, String path, ChannelHandlerContext ctx) {

        if (HttpUtil.is100ContinueExpected(req)) {
            HttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, CONTINUE);
            ctx.write(res);
            return Optional.of(res);
        }
        try {
            return nettishes.stream()
                .filter(nettish -> nettish.shouldHandle(path))
                .map(nettish -> nettish.handle(req, path, ctx))
                .flatMap(Optional::stream)
                .findFirst()
                .or(() ->
                    Optional.of(respond(ctx, BAD_REQUEST)));
        } catch (Exception e) {
            log.error("Failed: {}", req, e);
            return Optional.of(respond(ctx, INTERNAL_SERVER_ERROR));
        }
    }
}
