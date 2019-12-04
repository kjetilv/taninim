package mediaserver.gui;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Debug extends Nettish {

    private static final Logger log = LoggerFactory.getLogger(Debug.class);

    public Debug() {

        super("/debug");
    }

    @Override
    public HttpResponse handle(FullHttpRequest req, String path, ChannelHandlerContext ctx) {
        log.info("Request recieved @ {}: {}", path, req);
        return super.handle(req, path, ctx);
    }
}
