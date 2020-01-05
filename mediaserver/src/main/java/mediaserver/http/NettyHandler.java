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

        return pages.isEmpty() ||
            pages.stream().anyMatch(page -> matchingPage(webPath, page));
    }

    public abstract Handling handleRequest(WebPath webPath);

    protected Handling handled(HttpResponse sentResponse) {

        return Handling.sentResponse(this, sentResponse);
    }

    protected Handling respondPath(WebPath webPath, HttpResponse response) {

        return handled(Netty.respond(webPath.getCtx(), response));
    }

    protected Handling handle(WebPath webPath, byte[] bytes) {

        try {

            return respondPath(webPath, Netty.response(webPath, bytes, null));
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

        return respondPath(webPath, Netty.response(null, status, null, null));
    }

    private boolean matchingPage(WebPath webPath, Page page) {

        return webPath.isFor(page) && page.accessibleWith(webPath.getAccessLevel());
    }

}
