package mediaserver;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

class MediaServerRouter extends SimpleChannelInboundHandler<HttpRequest> {

    private static final Logger log = LoggerFactory.getLogger(MediaServerRouter.class);

    private final DirectoryLister directoryLister;

    MediaServerRouter(DirectoryLister directoryLister) {
        this.directoryLister = directoryLister;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpRequest req) {
        if (!req.decoderResult().isSuccess()) {
            FileStreamer.sendError(ctx, BAD_REQUEST);
            return;
        }
        String uri = req.uri();
        if (uri.isEmpty()) {
            FileStreamer.sendError(ctx, FORBIDDEN);
            return;
        }
        String path = URLDecoder.decode(uri, StandardCharsets.UTF_8);

        if (path.startsWith("/audio/")) {
            File file = FileStreamer.stream(req, path, ctx);
            log.info("Streamed file {}", file);
        }
        if (path.startsWith("/directory/")) {
            Object response = directoryLister.list(req, ctx);
            log.info("Listed directory {}", response);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        try {
            log.error("Caught error", cause);
        } finally {
            if (ctx.channel().isActive()) {
                FileStreamer.sendError(ctx, INTERNAL_SERVER_ERROR);
            }
        }
    }

}
