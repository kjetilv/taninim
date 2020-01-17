package mediaserver;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.ssl.NotSslRecordException;
import mediaserver.debug.Debug;
import mediaserver.debug.Exchange;
import mediaserver.http.*;
import mediaserver.sessions.Sessions;
import mediaserver.toolkit.Templater;
import mediaserver.util.RingBuffer;
import mediaserver.util.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLHandshakeException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

@ChannelHandler.Sharable
final class Router extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final int MAX_ROUTED = 1_024;

    private static final Logger log = LoggerFactory.getLogger(Router.class);

    private static final AtomicLong sequence = new AtomicLong();

    private final Sessions sessions;

    private final Clock clock;

    private final Map<String, Instant> routed = new ConcurrentHashMap<>();

    private final Collection<NettyHandler> handlers;

    private final RingBuffer<Exchange> latestExchanges = new RingBuffer<>(EXCHANGES_REMEMBERED);

    private static final int EXCHANGES_REMEMBERED = 100;

    Router(Sessions sessions, Templater templater, Clock clock, NettyHandler... handlers) {

        this(sessions, templater, clock, Arrays.asList(handlers));
    }

    Router(Sessions sessions, Templater templater, Clock clock, Collection<NettyHandler> handlers) {

        this.sessions = sessions;
        this.clock = clock;
        this.handlers = Stream.concat(
            Stream.of(new Debug(templater, latestExchanges::get)),
            handlers.stream()
        ).collect(Collectors.toList());
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {

        Instant time = clock.instant();
        Handling response = null;
        try {
            try {
                response = response(time, request, ctx);
            } catch (Throwable e) {
                logError("Handling of request failed", request, time, e);
                response = statusSent(ctx, INTERNAL_SERVER_ERROR);
            }
        } finally {
            if (response != null) {
                logExchange(time, response);
            }
        }
    }

    public Handling response(Instant time, FullHttpRequest request, ChannelHandlerContext ctx) {

        if (HttpUtil.is100ContinueExpected(request)) {
            return statusSent(ctx, CONTINUE);
        }

        return WebPath.from(ctx, request, time)
            .map(sessions::instrument)
            .flatMap(this::candidateHandlingStream)
            .findFirst()
            .map(result ->
                logged(request, result, time))
            .orElseGet(() ->
                loggedError(time, request, ctx));
    }

    public Handling loggedError(Instant time, FullHttpRequest request, ChannelHandlerContext ctx) {

        logError("No handling found", request, time, null);
        return statusSent(ctx, BAD_REQUEST);
    }

    public Stream<Handling> candidateHandlingStream(WebPath webPath) {

        return handlers.stream()
            .filter(handler ->
                handler.couldHandle(webPath))
            .map(handler ->
                handler.handleRequest(webPath))
            .filter(Handling::isHandled);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

        try {
            IgnoreLevel ignoreLevel = ignoredException(ctx, cause);
            switch (ignoreLevel) {
                case MEH:
                    log.info("Common error: {}", cause.toString());
                case SUMMARIZE:
                    log.warn("Sspurious error: {}", summarize(cause));
                    return;
                default:
                    log.error("Unknown error {}->{}",
                        addr(ctx, Channel::remoteAddress), addr(ctx, Channel::localAddress), cause);
            }
        } catch (Exception e) {
            log.error("Handling failed with {}", e.toString(), cause);
            log.error("Handling of {} failed", cause.toString(), e);
        } finally {
            if (ctx.channel().isActive()) {
                Netty.respond(ctx, INTERNAL_SERVER_ERROR);
            }
        }
    }

    public String summarize(Throwable cause) {

        return Throwables.causes(cause)
            .map(String::valueOf)
            .collect(Collectors.joining(" <= "));
    }

    private static Handling statusSent(ChannelHandlerContext ctx, HttpResponseStatus status) {

        return Handling.sentResponse(null, null, Netty.respond(ctx, status));
    }

    private void logExchange(Instant time, Handling handling) {

        if (handling != null) {
            WebPath webPath = handling.getWebPath();
            if (webPath != null && webPath.getSession() != null && webPath.getPage() != Page.DEBUG) {
                latestExchanges.add(new Exchange(time, sequence.getAndIncrement(), handling));
            }
        }
    }

    private void logError(String situation, FullHttpRequest req, Instant time, Throwable e) {

        log.error("Failure situation: {} [{}ms]: {}", situation, durationSince(time), req, e);
    }

    private Handling logged(FullHttpRequest req, Handling handling, Instant time) {

        synchronized (routed) {
            try {
                if (routed.containsKey(req.uri())) {
                    return handling;
                }
                routed.put(req.uri(), time);
                log.info("{} -> {} in {}ms",
                    req.uri(), handling.getSentResponse().status(), durationSince(time));
            } finally {
                if (routed.size() > MAX_ROUTED) {
                    routed.keySet().stream().findFirst().ifPresent(routed::remove);
                }
            }
        }
        return handling;
    }

    private long durationSince(Instant start) {

        return Duration.between(start, clock.instant()).toMillis();
    }

    private IgnoreLevel ignoredException(ChannelHandlerContext ctx, Throwable cause) {

        boolean testing = Optional.ofNullable(ctx.channel())
            .map(Channel::localAddress)
            .map(Object::toString)
            .filter(s -> s.contains("/0:0:0:0:0:0:0:1:"))
            .isPresent();
        return Optional.of(cause).stream()
            .flatMap(throwable -> {
                for (Throwable t = throwable; t != null && t.getCause() != t; t = t.getCause()) {
                    String message = throwable.getMessage();
                    if (throwable instanceof SocketException &&
                        message != null &&
                        message.equalsIgnoreCase("Connection reset")
                    ) {
                        return Stream.of(IgnoreLevel.SUMMARIZE);
                    }
                    if (
                        (is(SSLHandshakeException.class, throwable) || is(
                            NotSslRecordException.class, throwable)
                        ) && testing
                    ) {
                        return Stream.of(IgnoreLevel.MEH);
                    }
                }
                return Stream.empty();
            })
            .findFirst()
            .orElse(IgnoreLevel.LOG);
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

    private enum IgnoreLevel {

        LOG, SUMMARIZE, MEH
    }

}
