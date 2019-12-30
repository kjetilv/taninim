package mediaserver.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

public abstract class NettyHandler {

    private final Collection<Prefix> handled;

    protected NettyHandler(Prefix... handled) {

        this.handled = new HashSet<>(Arrays.asList(handled));
    }

    public boolean couldHandle(WebPath webPath) {

        return handled.isEmpty() || handled.stream().anyMatch(webPath::hasPrefix);
    }

    public abstract Handling handleRequest(WebPath webPath, ChannelHandlerContext ctx);

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

    protected Handling pass() {

        return Handling.pass(this);
    }

    protected Handling respondNotFound(ChannelHandlerContext ctx) {

        return sendResponse(ctx, NOT_FOUND);
    }

    protected Handling respondUnauthorized(ChannelHandlerContext ctx) {

        return sendResponse(ctx, UNAUTHORIZED);
    }

    protected Handling respondBadRequest(ChannelHandlerContext ctx) {

        return sendResponse(ctx, BAD_REQUEST);
    }

    protected Handling respondUnavailable(ChannelHandlerContext ctx) {

        return sendResponse(ctx, SERVICE_UNAVAILABLE);
    }

    protected Handling respondOK(ChannelHandlerContext ctx) {

        return sendResponse(ctx, OK);
    }

    private Handling sendResponse(ChannelHandlerContext ctx, HttpResponseStatus status) {

        return sendResponse(ctx, Netty.response(null, status, null, null));
    }
}
