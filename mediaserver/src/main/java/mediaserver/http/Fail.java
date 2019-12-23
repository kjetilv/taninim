package mediaserver.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;

public final class Fail extends NettyHandler {

    @Override
    public Handling handleRequest(FullHttpRequest req, WebPath webPath, ChannelHandlerContext ctx) {

        return respond(ctx, SERVICE_UNAVAILABLE);
    }
}
