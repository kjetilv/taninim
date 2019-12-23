package mediaserver.gui;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import mediaserver.http.Handling;
import mediaserver.http.NettyHandler;
import mediaserver.http.Prefix;
import mediaserver.http.WebPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Debug extends NettyHandler {

    private static final Logger log = LoggerFactory.getLogger(Debug.class);

    public Debug() {

        super(Prefix.DEBUG);
    }

    @Override
    public Handling handleRequest(FullHttpRequest req, WebPath webPath, ChannelHandlerContext ctx) {

        log.info("Request receieved @ {}: {}", webPath, req);
        return Handling.pass();
    }
}
