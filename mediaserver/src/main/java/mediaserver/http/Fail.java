package mediaserver.http;

import io.netty.channel.ChannelHandlerContext;

public final class Fail extends NettyHandler {

    @Override
    public Handling handleRequest(WebPath webPath, ChannelHandlerContext ctx) {

        return handleUnavailable(ctx);
    }
}
