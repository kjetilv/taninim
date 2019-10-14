package mediaserver.gui;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import mediaserver.util.IO;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class Resources extends Nettish {

    private final Map<String, Optional<HttpResponse>> cache = new ConcurrentHashMap<>();

    private static final String FAVICON_ICO = "/favicon.ico";

    public Resources(IO io) {
        super(io, "/resources", FAVICON_ICO);
    }

    @Override
    public HttpResponse handle(HttpRequest req, String path, ChannelHandlerContext ctx) {
        String resource = path.startsWith(FAVICON_ICO)
            ? path.substring(0, FAVICON_ICO.length())
            : resource(path);
        return cache.computeIfAbsent(resource, read(req))
            .map(response ->
                respond(ctx, response))
            .orElseGet(() ->
                respond(ctx, HttpResponseStatus.BAD_REQUEST));
    }

    private Function<String, Optional<HttpResponse>> read(HttpRequest req) {
        return path ->
            readBytes(path.substring(1)).map(bytes ->
                response(req, contentType(path), bytes, IMMUTABLE));
    }

    private static String contentType(String path) {
        return path.endsWith(".css") ? "text/css"
            : path.endsWith(".js") ? "text/javascript"
            : path.endsWith(".ico") ? "image/x-icon"
            : "text/plain";
    }
}
