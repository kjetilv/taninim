package mediaserver.http;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.Objects;
import javax.annotation.Nonnull;

import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

import static io.netty.handler.codec.http.HttpHeaderNames.CACHE_CONTROL;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;

public abstract class NettyHandler {

    protected static @Nonnull Headers cacheable(Duration duration) {
        return headers ->
            headers.set(
                CACHE_CONTROL,
                MessageFormat.format("public, max-age={0}", duration.toSeconds()));
    }

    private final Route route;

    protected NettyHandler(@Nonnull Route route) {
        this.route = Objects.requireNonNull(route, "page");
    }

    public @Nonnull Route getRoute() {
        return route;
    }

    public final @Nonnull Handling handleRequest(Req req) {
        if (req.isFor(route)) {
            return handle(req);
        }
        throw new IllegalArgumentException(this + " cannot serve " + req);
    }

    protected final @Nonnull Handling handle(Req req, HttpResponseStatus status) {
        return handle(req, Netty.response(null, status, null, null));
    }

    protected abstract @Nonnull Handling handle(Req req);

    protected final @Nonnull Handling handle(Req req, byte[] bytes, Headers cacheable) {
        try {
            return handle(req, Netty.response(req, bytes, cacheable));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to respond to " + req, e);
        }
    }

    protected final @Nonnull Handling handle(Req req, HttpResponse response) {
        return handled(req, Netty.respond(req.getCtx(), response));
    }

    protected final @Nonnull Handling handled(Req req, HttpResponse sentResponse) {
        return Handling.sentResponse(this, req, sentResponse);
    }

    protected final @Nonnull Handling handleBadRequest(Req req) {
        return handle(req, BAD_REQUEST);
    }

    protected final @Nonnull Handling handleNotFound(Req req) {
        return handle(req, NOT_FOUND);
    }

    protected final @Nonnull Handling handleUnauthorized(Req req) {
        return handle(req, UNAUTHORIZED);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + route + ']';
    }
}
