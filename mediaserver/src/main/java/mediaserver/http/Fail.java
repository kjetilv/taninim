package mediaserver.http;

import io.netty.channel.ChannelHandlerContext;

import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;

public final class Fail extends NettyHandler {

    @Override
    public Handling handleRequest(WebPath webPath, ChannelHandlerContext ctx) {

        return sendResponse(ctx, SERVICE_UNAVAILABLE);
    }
}
