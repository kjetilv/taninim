package mediaserver.http;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

public abstract class NettyHandler {

    private final Collection<Page> pages;

    protected static final Headers CACHEABLE = headers ->
        headers.accept(HttpHeaderNames.CACHE_CONTROL, "public, max-age=" + Duration.ofDays(1).toSeconds());

    protected NettyHandler(Page... pages) {

        this.pages = new HashSet<>(Arrays.asList(pages));
    }

    public final Optional<Handling> handledRequest(Req req) {

        if (pages.isEmpty() || pages.stream().anyMatch(req::isFor)) {
            return Optional.of(handleRequest(req));
        }
        return Optional.empty();
    }

    protected abstract Handling handleRequest(Req req);

    protected final Handling respond(Req req, HttpResponse response) {

        return handled(req, Netty.respond(req.getCtx(), response));
    }

    protected final Handling handled(Req req, HttpResponse sentResponse) {

        return Handling.sentResponse(this, req, sentResponse);
    }

    protected final Handling handle(Req req, byte[] bytes) {

        return handle(req, bytes, null);
    }

    protected final Handling handle(
        Req req,
        byte[] bytes,
        Headers cacheable
    ) {

        try {
            return respond(req, Netty.response(req, bytes, cacheable));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to respond to " + req, e);
        }
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

    private Handling handle(Req req, HttpResponseStatus status) {

        return respond(req, Netty.response(null, status, null, null));
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "" +
            "[" + pages.stream().map(Page::getPref).collect(Collectors.joining(" ")) + "]";
    }
}
