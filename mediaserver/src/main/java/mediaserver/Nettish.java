package mediaserver;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION;

public abstract class Nettish {

    private final String prefix;

    Nettish(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    static void respond(ChannelHandlerContext ctx, HttpResponse response) {
        ctx.writeAndFlush(response)
            .addListener(ChannelFutureListener.CLOSE);
    }

    static void respond(ChannelHandlerContext ctx, HttpResponseStatus response) {
        respond(ctx, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, response));
    }

    static Runnable reset(ChannelHandlerContext ctx) {
        return () ->
            respond(ctx, new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.FOUND,
                Unpooled.buffer(0),
                new DefaultHttpHeaders()
                    .set(LOCATION, "/"),
                EmptyHttpHeaders.INSTANCE));
    }

    abstract void handle(HttpRequest req, String path, ChannelHandlerContext ctx);
}
