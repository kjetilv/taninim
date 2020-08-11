package mediaserver;

import java.net.SocketAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import mediaserver.debug.Debug;
import mediaserver.debug.Exchange;
import mediaserver.gui.Login;
import mediaserver.http.Handling;
import mediaserver.http.Netty;
import mediaserver.http.NettyHandler;
import mediaserver.http.Req;
import mediaserver.http.Route;
import mediaserver.http.Route.Method;
import mediaserver.sessions.AccessLevel;
import mediaserver.sessions.Sessions;
import mediaserver.toolkit.Templater;
import mediaserver.util.RingBuffer;
import mediaserver.util.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;

@ChannelHandler.Sharable
final class Router extends SimpleChannelInboundHandler<FullHttpRequest> {
    
    private static final Logger log = LoggerFactory.getLogger(Router.class);
    
    private final Sessions sessions;
    
    private final Clock clock;
    
    private final Map<Route, NettyHandler> handlers;
    
    private final RingBuffer<Exchange> latestExchanges = new RingBuffer<>(EXCHANGES_REMEMBERED);
    
    Router(Sessions sessions, Templater templater, Clock clock, NettyHandler... handlers) {
        
        this.sessions = sessions;
        this.clock = clock;
        this.handlers = Stream.concat(
            Arrays.stream(handlers),
            Stream.of(new Debug(
                new Route("debug", AccessLevel.ADMIN, Method.GET),
                templater, latestExchanges::get))
        ).collect(Collectors.toMap(
            NettyHandler::getRoute,
            Function.identity(),
            (nh1, nh2) -> {
                throw new IllegalStateException("No merge: " + nh1 + "/" + nh2);
            },
            LinkedHashMap::new
        ));
    }
    
    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        
        if (HttpUtil.is100ContinueExpected(request)) {
            Netty.respond(ctx, CONTINUE);
            return;
        }
        
        try {
            handle(ctx, request);
        } catch (Throwable e) {
            logError(request, null, e);
            Netty.respond(ctx, INTERNAL_SERVER_ERROR);
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        
        try {
            switch (Exceptions.seriosityLevl(ctx, cause)) {
                case MEH:
                    log.info("Common error in {}: {}", ctx, summarize(cause));
                case CLIENT_WHIM:
                    log.info("Client slip-up in {}: {}", ctx, cause.toString());
                    return;
                case SUMMARIZE:
                    log.warn("Harmless-looking error in {}: {}", ctx, cause.toString());
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
    
    private void handle(ChannelHandlerContext ctx, FullHttpRequest request) {
        Instant time = clock.instant();
        handlers.keySet().stream()
            .filter(resolves(request))
            .max(prefixLength())
            .ifPresentOrElse(
                route ->
                    Req.from(route, ctx, request, time)
                        .map(sessions.binder())
                        .map(handler(ctx, time))
                        .orElseGet(() -> () -> {
                            log.info("Unauthenticated: {}", request);
                            Netty.respond(ctx, UNAUTHORIZED);
                        })
                        .run(),
                () -> {
                    log.info("Not handled: {}", request);
                    Netty.respond(ctx, NOT_FOUND);
                });
    }
    
    private Function<Req, Runnable> handler(ChannelHandlerContext ctx, Instant time) {
        return req ->
            boundRequestHandler(ctx, time, req);
    }
    
    private Runnable boundRequestHandler(ChannelHandlerContext ctx, Instant time, Req req) {
        try {
            if (req.hasRoute()) {
                NettyHandler nettyHandler = handlers.get(req.getRoute());
                if (nettyHandler == null) {
                    logError(req, time, null);
                    return () -> Netty.respond(ctx, NOT_FOUND);
                }
                return () -> {
                    Handling handled = nettyHandler.handleRequest(req);
                    logExchange(handled);
                };
            }
            if (!req.isBound() && req.getRoute().accessibleWith(AccessLevel.LOGIN)) {
                return () -> {
                    log.info("Redirected to login: {}", req);
                    Netty.respond(ctx, Netty.redirect(login()
                        .map(NettyHandler::getRoute)
                        .orElseGet(() ->
                            new Route("", AccessLevel.NONE, Method.GET))));
                };
            }
            return () -> {
                log.info("Unauthorized to login: {}", req);
                Netty.respond(ctx, UNAUTHORIZED);
            };
        } catch (Throwable e) {
            return () -> {
                logError(req, time, e);
                Netty.respond(ctx, BAD_REQUEST);
            };
        }
    }
    
    private Optional<NettyHandler> login() {
        return handlers.values().stream().filter(Login.class::isInstance).findFirst();
    }
    
    private void logExchange(Handling handling) {
        latestExchanges.add(new Exchange(sequence.getAndIncrement(), handling));
    }
    
    private void logError(Object req, Instant time, Throwable e) {
        log.error("Failure situation: {} [{}ms]: {}", "Handling of request failed", durationSince(time), req, e);
    }
    
    private long durationSince(Instant start) {
        return start == null ? -1L : Duration.between(start, clock.instant()).toMillis();
    }
    
    private static final AtomicLong sequence = new AtomicLong();
    
    private static final int EXCHANGES_REMEMBERED = 100;
    
    private static Comparator<Route> prefixLength() {
        
        return Comparator.comparing(Route::getPrefixLength);
    }
    
    private static Predicate<Route> resolves(HttpRequest request) {
        
        return handler ->
            handler.resolves(request.uri());
    }
    
    private static String summarize(Throwable cause) {
        
        return Throwables.causes(cause)
            .map(String::valueOf)
            .collect(Collectors.joining(" <= "));
    }
    
    private static String addr(ChannelHandlerContext ctx, Function<Channel, SocketAddress> remoteAddress) {
        
        return Optional.of(ctx)
            .map(ChannelHandlerContext::channel)
            .map(remoteAddress)
            .map(Objects::toString)
            .orElse("?");
    }
}
