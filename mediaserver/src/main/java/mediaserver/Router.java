package mediaserver;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.ssl.NotSslRecordException;
import mediaserver.http.Handling;
import mediaserver.http.NettyHandler;
import mediaserver.http.WebPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLHandshakeException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static mediaserver.http.Netty.respond;

final class Router extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final DefaultFullHttpResponse TO_BE_CONTINUED = new DefaultFullHttpResponse(HTTP_1_1, CONTINUE);

    private static final Logger log = LoggerFactory.getLogger(Router.class);

    private static final Collection<String> ROUTED = new ConcurrentSkipListSet<>();

    private final Collection<NettyHandler> handlers;

    Router(NettyHandler... handlers) {
        this(Arrays.asList(handlers));
    }

    Router(Collection<NettyHandler> handlers) {

        this.handlers = List.copyOf(handlers);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) {

        if (HttpUtil.is100ContinueExpected(req)) {
            respond(ctx, TO_BE_CONTINUED);
        }
        handle(ctx, req, handler(ctx));
    }

    private Function<WebPath, Optional<Handling>> handler(ChannelHandlerContext ctx) {

        return webPath ->
            handlers.stream()
                .filter(handler ->
                    handler.couldHandle(webPath))
                .map(handler ->
                    handler.handleRequest(webPath, ctx))
                .filter(Handling::isHandled)
                .findFirst();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

        try {
            ignoredException(ctx, cause).ifPresentOrElse(
                ignored ->
                    log.info("{}->{}: {}",
                        addr(ctx, Channel::remoteAddress), addr(ctx, Channel::localAddress), ignored.toString()),
                () ->
                    log.error("Caught unknown error {}->{}",
                        addr(ctx, Channel::remoteAddress), addr(ctx, Channel::localAddress), cause));
        } finally {
            if (ctx.channel().isActive()) {
                respond(ctx, INTERNAL_SERVER_ERROR);
            }
        }
    }

    private static void handle(
        ChannelHandlerContext ctx,
        FullHttpRequest req,
        Function<WebPath, Optional<Handling>> handler
    ) {

        Instant start = Instant.now();
        try {
            WebPath.from(req)
                .flatMap(handler)
                .ifPresentOrElse(
                    handling ->
                        reportHandled(handling, req, start),
                    () -> {
                        logError(req, null, durationSince(start));
                        respond(ctx, BAD_REQUEST);
                    });
        } catch (Exception e) {
            logError(req, e, durationSince(start));
            respond(ctx, INTERNAL_SERVER_ERROR);
        }
    }

    private static void reportHandled(Handling handling, FullHttpRequest req, Instant start) {

        log(req, handling, durationSince(start));
    }

    private static void logError(FullHttpRequest req, Exception e, long ms) {

        log.error("Failed [{}ms]: {}", ms, req, e);
    }

    private static void log(FullHttpRequest req, Handling response, long ms) {

        if (ROUTED.add(req.uri())) {
            log.info("{} -> {} in {}ms",
                req.uri(), response.getSentResponse().status(), ms);
        }
    }

    private static long durationSince(Instant start) {

        return Duration.between(start, Instant.now()).toMillis();
    }

    private Optional<Throwable> ignoredException(ChannelHandlerContext ctx, Throwable cause) {

        boolean testing = ctx.channel().localAddress().toString().contains("/0:0:0:0:0:0:0:1:");
        return Optional.of(cause).filter(throwable -> {
            for (Throwable t = throwable; t != null && t.getCause() != t; t = t.getCause()) {
                if (throwable instanceof SocketException &&
                    throwable.getMessage().equalsIgnoreCase("Connection reset")
                ) {
                    return true;
                }
                if ((is(SSLHandshakeException.class, throwable) ||
                    is(NotSslRecordException.class, throwable)) && testing) {
                    return true;
                }
            }
            return false;
        });
    }

    private boolean is(Class<?> sslHandshakeExceptionClass, Throwable e) {

        return e.getMessage().contains(sslHandshakeExceptionClass.getName());
    }

    private String addr(ChannelHandlerContext ctx, Function<Channel, SocketAddress> remoteAddress) {

        return Optional.of(ctx)
            .map(ChannelHandlerContext::channel)
            .map(remoteAddress)
            .map(Objects::toString)
            .orElse("?");
    }
}
