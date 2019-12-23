package mediaserver.gui;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
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
    public Handling handleRequest(FullHttpRequest req, WebPath webPath, ChannelHandlerContext ctx) {

        return webCache.get(resource)
            .map(respondBytes(req, webPath, ctx))
            .orElseGet(() ->
                respond(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE));
    }
}
