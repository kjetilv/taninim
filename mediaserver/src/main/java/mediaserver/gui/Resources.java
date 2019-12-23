package mediaserver.gui;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import mediaserver.http.*;

import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

public final class Resources extends NettyHandler {

    private final String resourcePrefix;

    private final WebCache<String, byte[]> cache;

    public Resources(String resourcePrefix, WebCache<String, byte[]> cache) {

        super(Prefix.RES);
        this.resourcePrefix = resourcePrefix;
        this.cache = cache;
    }

    @Override
    public Handling handleRequest(FullHttpRequest req, WebPath webPath, ChannelHandlerContext ctx) {

        return cache.get(resourcePrefix + webPath.getPath())
            .map(respondBytes(req, webPath, ctx))
            .orElseGet(() ->
                respond(ctx, NOT_FOUND));
    }
}
