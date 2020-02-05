package mediaserver.http;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

public abstract class NettyHandler {

    private final Collection<Page> pages;

    protected static final Headers CACHEABLE = headers ->
        headers.accept(HttpHeaderNames.CACHE_CONTROL, "public, max-age=" + Duration.ofDays(1).toSeconds());

    protected NettyHandler() {

        this(null);
    }

    protected NettyHandler(Page page) {

        this.pages = page == null ? Collections.emptyList() : Collections.singleton(page);
    }

    public final Optional<Handling> handledRequest(Req req) {

        if (pages.isEmpty() || pages.stream().anyMatch(req::isFor)) {
            return Optional.of(handleRequest(req));
        }
        return Optional.empty();
    }

    protected final Handling handle(Req req, HttpResponseStatus status) {

        return handle(req, Netty.response(null, status, null, null));
    }

    protected abstract Handling handleRequest(Req req);

    protected final Handling handle(Req req, HttpResponse response) {

        return Handling.sentResponse(this, req, Netty.respond(req.getCtx(), response));
    }

    protected final Handling handled(Req req, HttpResponse sentResponse) {

        return Handling.sentResponse(this, req, sentResponse);
    }

    protected final Handling handle(Req req, byte[] bytes, Headers cacheable) {

        try {
            return handle(req, Netty.response(req, bytes, cacheable));
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

    @Override
    public String toString() {

        return getClass().getSimpleName() + "" +
            "[" + pages.stream().map(Page::getPref).collect(Collectors.joining(" ")) + "]";
    }
}
