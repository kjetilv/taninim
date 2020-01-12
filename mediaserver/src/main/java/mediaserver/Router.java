package mediaserver;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.NotSslRecordException;
import mediaserver.gui.TemplateEnabled;
import mediaserver.gui.Templater;
import mediaserver.http.*;
import mediaserver.sessions.Session;
import mediaserver.sessions.Sessions;
import mediaserver.util.RingBuffer;
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
import java.util.function.Supplier;
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
        Handling response;
        try {
            response = response(time, request, ctx);
        } catch (Throwable e) {
            logError("Handling of request failed", request, time, e);
            response = statusSent(ctx, INTERNAL_SERVER_ERROR);
        }
        logExchange(time, response);
    }

    public Handling response(Instant time, FullHttpRequest request, ChannelHandlerContext ctx) {

        if (HttpUtil.is100ContinueExpected(request)) {
            return statusSent(ctx, CONTINUE);
        }

        Stream<Handling> handling = WebPath.from(ctx, request, time)
            .map(sessions::instrument)
            .flatMap(webPath ->
                handlers.stream()
                    .filter(handler ->
                        handler.couldHandle(webPath))
                    .map(handler ->
                        handler.handleRequest(webPath))
                    .filter(Handling::isHandled))
            .limit(1);

        return handling
            .peek(result ->
                log(request, result, time))
            .findFirst()
            .orElseGet(() -> {
                logError("No handling found", request, time, null);
                return statusSent(ctx, BAD_REQUEST);
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

    private static Handling statusSent(ChannelHandlerContext ctx, HttpResponseStatus status) {

        return Handling.sentResponse(null, null, Netty.respondRaw(ctx, status));
    }

    private void logExchange(Instant time, Handling handling) {

        WebPath webPath = handling.getWebPath();
        if (webPath != null && webPath.getSession() != null && webPath.getPage() != Page.DEBUG) {
            latestExchanges.add(new Exchange(time, sequence.getAndIncrement(), handling));
        }
    }

    private void logError(String situation, FullHttpRequest req, Instant time, Throwable e) {

        log.error("Failure situation: {} [{}ms]: {}", situation, durationSince(time), req, e);
    }

    private void log(FullHttpRequest req, Handling response, Instant time) {

        synchronized (routed) {
            try {
                if (routed.containsKey(req.uri())) {
                    return;
                }
                routed.put(req.uri(), time);
                log.info("{} -> {} in {}ms",
                    req.uri(), response.getSentResponse().status(), durationSince(time));
            } finally {
                if (routed.size() > MAX_ROUTED) {
                    routed.keySet().stream().findFirst().ifPresent(routed::remove);
                }
            }
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

    private static final class Debug extends TemplateEnabled {

        private final Logger log = LoggerFactory.getLogger(Debug.class);

        private final Supplier<Collection<Exchange>> latestExchanges;

        public Debug(Templater templater, Supplier<Collection<Exchange>> latestExchanges) {

            super(templater, Page.DEBUG);
            this.latestExchanges = latestExchanges;
        }

        @Override
        public Handling handleRequest(WebPath webPath) {

            log.info("Request receieved @ {}: {}", webPath, webPath.getRequest());
            Map<Session, List<Exchange>> exchanges = latestExchanges.get().stream()
                .filter(exchange ->
                    exchange.getWebPath() != null)
                .collect(Collectors.groupingBy(
                    exchange ->
                        exchange.getWebPath().getSession(),
                    Collectors.toCollection(ArrayList::new)));
            return respondHtml(webPath, getTemplate(DEBUG_PAGE).add(
                "exchanges",
                exchanges
                    .entrySet()));
        }
    }

    public static final class Exchange {

        private final Instant time;

        private final long sequenceNo;

        private final NettyHandler handler;

        private final WebPath webPath;

        private final HttpResponse response;

        public Exchange(Instant time, long sequenceNo, Handling handling) {

            this.time = time;
            this.sequenceNo = sequenceNo;
            this.handler = handling.getHandler();
            this.webPath = handling.getWebPath();
            this.response = handling.getSentResponse();
        }

        public Instant getTime() {

            return time != null ? time
                : webPath != null ? webPath.getTime()
                : null;
        }

        public NettyHandler getHandler() {

            return handler;
        }

        public WebPath getWebPath() {

            return webPath;
        }

        public HttpRequest getRequest() {

            return webPath == null ? null : webPath.getRequest();
        }

        public HttpResponse getResponse() {

            return response;
        }

        public long getSequenceNo() {

            return sequenceNo;
        }

        @Override
        public int hashCode() {

            return (int) sequenceNo;
        }

        @Override
        public boolean equals(Object o) {

            return this == o || o instanceof Exchange && sequenceNo == ((Exchange) o).sequenceNo;
        }
    }
}
