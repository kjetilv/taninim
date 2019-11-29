package mediaserver.gui;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import mediaserver.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CookiesPlease extends Nettish {

    private static final Logger log = LoggerFactory.getLogger(CookiesPlease.class);

    public CookiesPlease() {

        super("/cookiesplease");
    }

    @Override
    public HttpResponse handle(FullHttpRequest req, String path, ChannelHandlerContext ctx) {

        return respond(ctx, okCookieResponse(req, path));
    }
}
