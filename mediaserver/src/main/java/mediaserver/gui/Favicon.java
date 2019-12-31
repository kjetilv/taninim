package mediaserver.gui;

import io.netty.channel.ChannelHandlerContext;
import mediaserver.http.*;

public final class Favicon extends NettyHandler {

    private final WebCache<String, byte[]> webCache;

    private final String resource;

    public Favicon(WebCache<String, byte[]> webCache, String resource) {

        super(Prefix.FAVICON_ICO);
        this.webCache = webCache;
        this.resource = resource;
    }

    @Override
    public Handling handleRequest(WebPath webPath, ChannelHandlerContext ctx) {

        return webCache.get(resource)
            .map(bytes ->
                handle(webPath, ctx, bytes))
            .orElseGet(() ->
                handleNotFound(ctx));
    }
}
