package mediaserver.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import mediaserver.gui.Template;
import mediaserver.util.IO;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

public abstract class NettyHandler {

    private final Collection<Prefix> handled;

    private final WebCache<String, String> cache;

    protected NettyHandler(Prefix... handled) {

        this.handled = new HashSet<>(Arrays.asList(handled));
        this.cache = new WebCache<>(IO::read);
    }

    public boolean couldHandle(WebPath webPath) {

        return handled.isEmpty() || handled.stream().anyMatch(webPath::hasPrefix);
    }

    public abstract Handling handleRequest(WebPath webPath, ChannelHandlerContext ctx);

    protected Handling sendResponse(ChannelHandlerContext ctx, HttpResponseStatus status) {

        return sendResponse(ctx, Netty.response(null, status, null, null));
    }

    protected Handling sendResponse(ChannelHandlerContext ctx, HttpResponse response) {

        return sentResponse(Netty.respond(ctx, response));
    }

    protected Handling sentResponse(HttpResponse response) {

        return Handling.sentResponse(this, response);
    }

    protected Handling handle(
        WebPath webPath,
        ChannelHandlerContext ctx,
        byte[] bytes
    ) {

        try {
            HttpResponse response =
                Netty.response(webPath, bytes, null);
            return sendResponse(ctx, response);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to respond to " + webPath, e);
        }
    }

    protected Template readTemplate(String resource) {

        return cache.get(resource)
            .map(source ->
                new Template(resource, source))
            .orElseThrow(() ->
                new IllegalArgumentException("No such template resource: " + resource));
    }

    protected Handling pass() {

        return Handling.pass(this);
    }

}
