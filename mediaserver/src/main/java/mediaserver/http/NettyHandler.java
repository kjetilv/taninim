package mediaserver.http;

import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

public abstract class NettyHandler {

    private final Collection<Page> pages;

    protected NettyHandler(Page... pages) {

        this.pages = new HashSet<>(Arrays.asList(pages));
    }

    public boolean couldHandle(WebPath webPath) {

        if (pages.isEmpty()) {
            return true;
        }
        return pages.stream()
            .filter(page ->
                matchingPage(webPath, page))
            .anyMatch(page ->
                page.accessibleIn(webPath.getSession()));
    }

    public abstract Handling handleRequest(WebPath webPath);

    protected Handling respond(WebPath webPath, HttpResponse response) {

        return handled(webPath, Netty.respond(webPath.getCtx(), response));
    }

    protected Handling handled(WebPath webPath, HttpResponse sentResponse) {

        return Handling.sentResponse(this, webPath, sentResponse);
    }

    protected Handling handle(WebPath webPath, byte[] bytes) {

        try {

            return respond(webPath, Netty.response(webPath, bytes, null));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to respond to " + webPath, e);
        }
    }

    protected Handling pass() {

        return Handling.pass(this);
    }

    protected Handling handleUnauthorized(WebPath webPath) {

        return handle(webPath, UNAUTHORIZED);
    }

    protected Handling handleBadRequest(WebPath webPath) {

        return handle(webPath, BAD_REQUEST);
    }

    protected Handling handleNotFound(WebPath webPath) {

        return handle(webPath, NOT_FOUND);
    }

    private Handling handle(WebPath webPath, HttpResponseStatus status) {

        return respond(webPath, Netty.response(null, status, null, null));
    }

    private boolean matchingPage(WebPath webPath, Page page) {

        return webPath.isFor(page) && page.accessibleWith(webPath.getAccessLevel());
    }

}
