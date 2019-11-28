package mediaserver.gui;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import mediaserver.util.IO;

public class Resources extends Nettish {

    private final WebCache<String, byte[]> cache;

    private static final String FAVICON_ICO = "/favicon.ico";

    public Resources(IO io) {

        super(io, "/res", FAVICON_ICO);
        cache = new WebCache<>(io::readBytes);
    }

    @Override
    public HttpResponse handle(FullHttpRequest req, String path, ChannelHandlerContext ctx) {

        try {
            String resource = "res/" + (path.startsWith(FAVICON_ICO)
                ? path.substring(0, FAVICON_ICO.length())
                : resource(path));
            return cache.get(resource)
                .map(bytes ->
                    response(req, null, contentType(path), bytes, IMMUTABLE))
                .map(response -> {
                    try {
                        return respond(ctx, response);
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to respond to " + path, e);
                    }
                })
                .orElseGet(() ->
                    respond(ctx, HttpResponseStatus.BAD_REQUEST));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to respond to " + path, e);
        }
    }

    private static String contentType(String path) {

        return path.endsWith(".css") ? "text/css"
            : path.endsWith(".js") ? "text/javascript"
            : path.endsWith(".ico") ? "image/x-icon"
            : "text/plain";
    }
}
