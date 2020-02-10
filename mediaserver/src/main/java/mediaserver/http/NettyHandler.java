package mediaserver.http;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.time.Duration;
import java.util.Objects;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

public abstract class NettyHandler {

    protected static final Headers CACHEABLE = headers ->
        headers.accept(HttpHeaderNames.CACHE_CONTROL, "public, max-age=" + Duration.ofDays(1).toSeconds());

    private final Route route;

    protected NettyHandler(Route route) {

        this.route = Objects.requireNonNull(route, "page");
    }

    public Route getRoute() {

        return route;
    }

    public final Handling handledRequest(Req req) {

        if (req.isFor(route)) {
            return handledRequest(req);
        }
        throw new IllegalArgumentException(this + " cannot serve " + req);
    }

    protected final Handling handle(Req req, HttpResponseStatus status) {

        return handle(req, Netty.response(null, status, null, null));
    }

    protected abstract Handling handleRequest(Req req);

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

    @Override
    public String toString() {

        return getClass().getSimpleName() + '[' + route + ']';
    }
}
