package mediaserver;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.ssl.NotSslRecordException;
import mediaserver.http.Handling;
import mediaserver.http.Netty;
import mediaserver.http.NettyHandler;
import mediaserver.http.WebPath;
import mediaserver.sessions.Sessions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLHandshakeException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

@ChannelHandler.Sharable
final class Router extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger log = LoggerFactory.getLogger(Router.class);

    private static final Collection<String> ROUTED = new ConcurrentSkipListSet<>();

    private final Sessions sessions;

    private final Clock clock;

    private final Collection<NettyHandler> handlers;

    Router(Sessions sessions, Clock clock, NettyHandler... handlers) {

        this(sessions, clock, Arrays.asList(handlers));
    }

    Router(Sessions sessions, Clock clock, Collection<NettyHandler> handlers) {

        this.sessions = sessions;
        this.clock = clock;
        this.handlers = List.copyOf(handlers);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {

        if (HttpUtil.is100ContinueExpected(request)) {
            Netty.respondRaw(ctx, CONTINUE);
        }
        Instant time = clock.instant();
        Optional<Handling> handling;
        try {
            handling = handling(ctx, request, time);
        } catch (Exception e) {
            logError("Handling of request failed", request, time, e);
            Netty.respondRaw(ctx, INTERNAL_SERVER_ERROR);
            return;
        }
        handling.ifPresentOrElse(
            result ->
                log(request, result, time),
            () -> {
                logError("No handling found", request, time, null);
                Netty.respondRaw(ctx, BAD_REQUEST);
            });
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
                Netty.respondRaw(ctx, INTERNAL_SERVER_ERROR);
            }
        }
    }

    private Optional<Handling> handling(ChannelHandlerContext ctx, FullHttpRequest request, Instant time) {

        return WebPath.from(ctx, request, time)
            .map(sessions::instrument)
            .flatMap(webPath ->
                handlers.stream()
                    .filter(handler ->
                        handler.couldHandle(webPath))
                    .map(handler ->
                        handler.handleRequest(webPath))
                    .filter(Handling::isHandled)
                    .findFirst());
    }

    private void logError(String situation, FullHttpRequest req, Instant time, Exception e) {

        log.error("Failure situation: {} [{}ms]: {}", situation, durationSince(time), req, e);
    }

    private void log(FullHttpRequest req, Handling response, Instant time) {

        if (ROUTED.add(req.uri())) {
            log.info("{} -> {} in {}ms",
                req.uri(), response.getSentResponse().status(), durationSince(time));
        }
    }

    private long durationSince(Instant start) {

        return Duration.between(start, clock.instant()).toMillis();
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
