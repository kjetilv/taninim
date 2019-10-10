package mediaserver;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import mediaserver.util.IO;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.netty.handler.codec.http.HttpHeaderNames.CACHE_CONTROL;

public class Resources extends Nettish {

    private final Map<String, Optional<HttpResponse>> cache = new ConcurrentHashMap<>();

    private static final Consumer<BiConsumer<CharSequence, CharSequence>> IMMUTABLE = headers ->
        headers.accept(CACHE_CONTROL, "immutable");

    Resources() {
        super("/resources");
    }

    @Override
    boolean handle(HttpRequest req, String path, ChannelHandlerContext ctx) {
        return cache.computeIfAbsent(path, read(req))
            .map(response ->
                respond(ctx, response))
            .isPresent();
    }

    private static Function<String, Optional<HttpResponse>> read(HttpRequest req) {
        return path ->
            IO.readBytes(path.substring(1)).map(bytes ->
                response(req, contentType(path), bytes, IMMUTABLE));
    }

    private static String contentType(String path) {
        return path.endsWith(".css") ? "text/css"
            : path.endsWith(".javascript") ? "text/javascript"
            : "text/plain";
    }
}
