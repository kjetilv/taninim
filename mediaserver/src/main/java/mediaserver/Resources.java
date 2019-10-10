package mediaserver;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import mediaserver.util.IO;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

public class Resources extends Nettish {

    private final Map<String, Optional<HttpResponse>> cache = new ConcurrentHashMap<>();

    private final IO io;

    Resources(IO io) {
        super(io, "/resources");
        this.io = io;
    }

    @Override
    void handle(HttpRequest req, String path, ChannelHandlerContext ctx) {
        cache.computeIfAbsent(path, read(req))
            .map(response ->
                respond(ctx, response));
    }

    private Function<String, Optional<HttpResponse>> read(HttpRequest req) {
        return path ->
            readBytes(path.substring(1)).map(bytes ->
                response(req, contentType(path), bytes, IMMUTABLE));
    }

    private static String contentType(String path) {
        return path.endsWith(".css") ? "text/css"
            : path.endsWith(".js") ? "text/javascript"
            : "text/plain";
    }
}
