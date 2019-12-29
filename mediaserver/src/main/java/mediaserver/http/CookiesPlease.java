package mediaserver.http;

import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CookiesPlease extends NettyHandler {

    private static final Logger log = LoggerFactory.getLogger(CookiesPlease.class);

    public CookiesPlease() {

        super(Prefix.COOKIESPLEASE);
    }

    @Override
    public Handling handleRequest(WebPath webPath, ChannelHandlerContext ctx) {

        return sendResponse(ctx, Netty.okCookieResponse(webPath, "cookies"));
    }
}
