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

    protected Handling handled(HttpResponse sentResponse) {

        return Handling.sentResponse(this, sentResponse);
    }

    protected Handling handle(WebPath webPath, ChannelHandlerContext ctx, byte[] bytes) {

        try {
            return handle(ctx, Netty.response(webPath, bytes, null));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to respond to " + webPath, e);
        }
    }

    protected Handling handle(ChannelHandlerContext ctx, HttpResponse response) {

        HttpResponse sentResponse = Netty.respond(ctx, response);
        return handled(sentResponse);
    }

    protected Handling handleNotFound(ChannelHandlerContext ctx) {

        return handle(ctx, NOT_FOUND);
    }

    protected Handling handleUnauthorized(ChannelHandlerContext ctx) {

        return handle(ctx, UNAUTHORIZED);
    }

    protected Handling handleBadRequest(ChannelHandlerContext ctx) {

        return handle(ctx, BAD_REQUEST);
    }

    protected Handling handleUnavailable(ChannelHandlerContext ctx) {

        return handle(ctx, SERVICE_UNAVAILABLE);
    }

    protected Handling handleOK(ChannelHandlerContext ctx) {

        return handle(ctx, OK);
    }

    protected Handling pass() {

        return Handling.pass(this);
    }

    private Handling handle(ChannelHandlerContext ctx, HttpResponseStatus status) {

        return handle(ctx, Netty.response(null, status, null, null));
    }
}
