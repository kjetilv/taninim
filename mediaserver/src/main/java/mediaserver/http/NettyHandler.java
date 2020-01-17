package mediaserver.http;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

public abstract class NettyHandler {

    private final Collection<Page> pages;

    protected static final Consumer<BiConsumer<CharSequence, CharSequence>> CACHEABLE =
        headers ->
            headers.accept(HttpHeaderNames.CACHE_CONTROL, "public");

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

        return handle(webPath, bytes, null);
    }

    protected Handling handle(
        WebPath webPath,
        byte[] bytes,
        Consumer<BiConsumer<CharSequence, CharSequence>> moreHeaders
    ) {

        try {
            return respond(webPath, Netty.response(webPath, bytes, moreHeaders));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to respond to " + webPath, e);
        }
    }

    protected Handling pass() {

        return Handling.pass(this);
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

    @Override
    public String toString() {

        return getClass().getSimpleName() + "" +
            "[" + pages.stream().map(Enum::name).collect(Collectors.joining(" ")) + "]";
    }
}
