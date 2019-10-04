package mediaserver;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

public class Nettish {
    static void respond(ChannelHandlerContext ctx, HttpResponse response) {
        ctx.writeAndFlush(response)
            .addListener(ChannelFutureListener.CLOSE);
    }

    static void respond(ChannelHandlerContext ctx, HttpResponseStatus response) {
        respond(ctx, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, response));
    }
}
