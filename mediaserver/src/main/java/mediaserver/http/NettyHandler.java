package mediaserver.http;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.Objects;

import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

import static io.netty.handler.codec.http.HttpHeaderNames.CACHE_CONTROL;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;

public abstract class NettyHandler {

    private final Route route;

    protected NettyHandler(Route route) {
        this.route = Objects.requireNonNull(route, "page");
    }

    public Route getRoute() {
        return route;
    }

    public final Handling handleRequest(Req req) {
        if (req.isFor(route)) {
            return handle(req);
        }
        throw new IllegalArgumentException(this + " cannot serve " + req);
    }

    protected final Handling handle(Req req, HttpResponseStatus status) {
        return handle(req, Netty.response(null, status, null, null));
    }

    protected abstract Handling handle(Req req);

    protected final Handling handle(Req req, byte[] bytes, Headers cacheable) {
        try {
            return handle(req, Netty.response(req, bytes, cacheable));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to respond to " + req, e);
        }
    }

    protected final Handling handle(Req req, HttpResponse response) {
        return handled(req, Netty.respond(req.getCtx(), response));
    }

    protected final Handling handled(Req req, HttpResponse sentResponse) {
        return Handling.sentResponse(this, req, sentResponse);
    }

    protected final Handling handleBadRequest(Req req) {
        return handle(req, BAD_REQUEST);
    }

    protected final Handling handleNotFound(Req req) {
        return handle(req, NOT_FOUND);
    }

    protected final Handling handleUnauthorized(Req req) {
        return handle(req, UNAUTHORIZED);
    }

    private static String age(Duration duration) {
        return MessageFormat.format("public, max-age={0}", duration.toSeconds());
    }

    protected static Headers cacheable(Duration duration) {
        return headers ->
            headers.set(CACHE_CONTROL, age(duration));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + route + ']';
    }
}
