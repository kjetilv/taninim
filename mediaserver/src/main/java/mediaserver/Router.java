package mediaserver;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static mediaserver.http.NettyHandler.respond;

final class Router extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger log = LoggerFactory.getLogger(Router.class);

    private static final Collection<String> ROUTED = new ConcurrentSkipListSet<>();

    private final Collection<NettyHandler> handlers;

    Router(NettyHandler... handlers) {

        this.handlers = Arrays.asList(handlers);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) {

        Instant start = Instant.now();
        Handling response = response(ctx, req);
        log(req, response, duration(start));
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

    private void log(FullHttpRequest req, Handling handling, long ms) {

        HttpResponse response = handling.getSentResponse();
        if (ROUTED.add(req.uri())) {
            log.info("{} -> {}/{} in {}ms",
                req.uri(), response.status(), response.headers().get(HttpHeaderNames.CONTENT_LENGTH), ms);
        }
    }

    private long duration(Instant start) {

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

    private Handling response(ChannelHandlerContext ctx, FullHttpRequest req) {

        if (HttpUtil.is100ContinueExpected(req)) {
            return toBeContinued(ctx);
        }
        String uri = req.uri();
        if (uri == null || uri.isBlank()) {
            return respond(ctx, BAD_REQUEST);
        }
        WebPath webPath = WebPath.build(uri);

        try {
            return handlers.stream()
                .filter(handler ->
                    handler.couldHandle(webPath))
                .map(handler ->
                    handler.handle(req, webPath, ctx))
                .filter(Handling::isDone)
                .findFirst()
                .orElseGet(() ->
                    respond(ctx, INTERNAL_SERVER_ERROR));
        } catch (Exception e) {
            log.error("Failed: {}", req, e);
            return respond(ctx, INTERNAL_SERVER_ERROR);
        }
    }

    private String addr(ChannelHandlerContext ctx, Function<Channel, SocketAddress> remoteAddress) {

        return Optional.of(ctx)
            .map(ChannelHandlerContext::channel)
            .map(remoteAddress)
            .map(Objects::toString)
            .orElse("?");
    }

    private static Handling toBeContinued(ChannelHandlerContext ctx) {

        HttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, CONTINUE);
        ctx.write(res);
        return Handling.sentResponse(res);
    }
}
