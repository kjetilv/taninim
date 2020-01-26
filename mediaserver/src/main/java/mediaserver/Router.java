package mediaserver;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import mediaserver.debug.Debug;
import mediaserver.debug.Exchange;
import mediaserver.http.*;
import mediaserver.sessions.AccessLevel;
import mediaserver.sessions.Sessions;
import mediaserver.toolkit.Templater;
import mediaserver.util.RingBuffer;
import mediaserver.util.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

@ChannelHandler.Sharable
final class Router extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger log = LoggerFactory.getLogger(Router.class);

    private static final AtomicLong sequence = new AtomicLong();

    private final Sessions sessions;

    private final Clock clock;

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

        if (HttpUtil.is100ContinueExpected(request)) {
            Netty.respond(ctx, CONTINUE);
            return;
        }

        Instant time = clock.instant();
        try {

            getReq(ctx, request, time).ifPresentOrElse(
                path ->
                    handle(ctx, path),
                () ->
                    error(ctx, request));

        } catch (Throwable e) {
            logError(request, time, e);
            Netty.respond(ctx, INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

        try {
            switch (Exceptions.ignoreLevel(ctx, cause)) {
                case MEH:
                    log.info("Common error in {}: {}", ctx, summarize(cause));
                case SUMMARIZE:
                    log.warn("Spurious error in {}", ctx, cause);
                    return;
                default:
                    log.error("Unknown error {}->{} in {}",
                        addr(ctx, Channel::remoteAddress), addr(ctx, Channel::localAddress), ctx, cause);
            }
        } catch (Exception e) {
            log.error("Handling of {}->{} failed with unhandled error {}",
                addr(ctx, Channel::remoteAddress), addr(ctx, Channel::localAddress), e.toString(), cause);
            log.error("Handling of {} failed",
                cause.toString(), e);
        } finally {
            if (ctx.channel().isActive()) {
                Netty.respond(ctx, INTERNAL_SERVER_ERROR);
            }
        }
    }

    private Optional<Req> getReq(ChannelHandlerContext ctx, FullHttpRequest request, Instant time) {

        return Req.from(ctx, request, time).map(sessions::bind);
    }

    private void handle(ChannelHandlerContext ctx, Req req) {

        if (req.isAllowed()) {

            handled(req).ifPresentOrElse(
                this::logExchange,
                () -> {
                    log.info("Failed: {}", req);
                    Netty.respond(ctx, BAD_REQUEST);
                });
        } else {

            handleUnauthorized(ctx, req);
        }
    }

    private void handleUnauthorized(ChannelHandlerContext ctx, Req req) {

        if (loginAllowed(req)) {

            log.info("Redirecteded: {}", req);
            Netty.respond(ctx, Netty.redirectResponse(Page.LOGIN));
        } else {

            log.info("Unauthorized: {}", req);
            Netty.respond(ctx, UNAUTHORIZED);
        }
    }

    private void error(ChannelHandlerContext ctx, FullHttpRequest request) {

        log.info("Failed: {}", request);
        Netty.respond(ctx, BAD_REQUEST);
    }

    private boolean loginAllowed(Req req) {

        return req.getPage() != Page.LOGIN && req.getPage().accessibleWith(AccessLevel.LOGIN);
    }

    private Optional<Handling> handled(Req req) {

        return handlers.stream()
            .flatMap(handler ->
                handler.handledRequest(req).stream())
            .filter(
                Handling::isHandled)
            .findFirst();
    }

    private String summarize(Throwable cause) {

        return Throwables.causes(cause)
            .map(String::valueOf)
            .collect(Collectors.joining(" <= "));
    }

    private void logExchange(Handling handling) {

        latestExchanges.add(new Exchange(sequence.getAndIncrement(), handling));
    }

    private void logError(FullHttpRequest req, Instant time, Throwable e) {

        log.error("Failure situation: {} [{}ms]: {}", "Handling of request failed", durationSince(time), req, e);
    }

    private long durationSince(Instant start) {

        return Duration.between(start, clock.instant()).toMillis();
    }

    private String addr(ChannelHandlerContext ctx, Function<Channel, SocketAddress> remoteAddress) {

        return Optional.of(ctx)
            .map(ChannelHandlerContext::channel)
            .map(remoteAddress)
            .map(Objects::toString)
            .orElse("?");
    }
}
