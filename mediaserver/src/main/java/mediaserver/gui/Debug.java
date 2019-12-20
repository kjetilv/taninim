package mediaserver.gui;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import mediaserver.http.Nettish;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class Debug extends Nettish {

    private static final Logger log = LoggerFactory.getLogger(Debug.class);

    public Debug() {

        super("/debug");
    }

    @Override
    public Optional<HttpResponse> handle(FullHttpRequest req, String path, ChannelHandlerContext ctx) {
        log.info("Request receieved @ {}: {}", path, req);
        return Optional.empty();
    }
}
