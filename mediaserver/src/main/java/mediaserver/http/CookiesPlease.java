package mediaserver.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class CookiesPlease extends Nettish {

    private static final Logger log = LoggerFactory.getLogger(CookiesPlease.class);

    public CookiesPlease() {

        super("/cookiesplease");
    }

    @Override
    public Optional<HttpResponse> handle(FullHttpRequest req, String path, ChannelHandlerContext ctx) {

        return Optional.of(respond(ctx, okCookieResponse(req, path)));
    }
}
