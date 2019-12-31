package mediaserver.gui;

import io.netty.channel.ChannelHandlerContext;
import mediaserver.http.*;

public final class Resources extends NettyHandler {

    private final String resourcePrefix;

    private final WebCache<String, byte[]> cache;

    public Resources(String resourcePrefix, WebCache<String, byte[]> cache) {

        super(Prefix.RES);
        this.resourcePrefix = resourcePrefix;
        this.cache = cache;
    }

    @Override
    public Handling handleRequest(WebPath webPath, ChannelHandlerContext ctx) {

        return cache.get(resourcePrefix + webPath.getPath())
            .map(bytes ->
                handle(webPath, ctx, bytes))
            .orElseGet(() ->
                handleNotFound(ctx));
    }
}
