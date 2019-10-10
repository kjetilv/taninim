package mediaserver;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import mediaserver.util.IO;

import java.net.URL;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public abstract class Nettish {

    private final IO io;

    private final String prefix;

    protected static final Consumer<BiConsumer<CharSequence, CharSequence>> IMMUTABLE = headers ->
        headers.accept(CACHE_CONTROL, "immutable");

    Nettish(IO io, String prefix) {
        this.io = io;
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    abstract void handle(HttpRequest req, String path, ChannelHandlerContext ctx);

    Template template(String resource) {
        return new Template(io, resource);
    }

    Optional<byte[]> readBytes(String resource) {
        return io.readBytes(resource);
    }

    URL resolve(String path) {
        return io.resolve(path);
    }

    static boolean respond(ChannelHandlerContext ctx, HttpResponse response) {
        ctx.writeAndFlush(response)
            .addListener(ChannelFutureListener.CLOSE);
        return true;
    }

    static void respond(ChannelHandlerContext ctx, HttpResponseStatus response) {
        respond(ctx, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, response));
    }

    static Runnable reset(ChannelHandlerContext ctx) {
        return () ->
            respond(ctx, new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.FOUND,
                Unpooled.buffer(0),
                new DefaultHttpHeaders()
                    .set(LOCATION, "/"),
                EmptyHttpHeaders.INSTANCE));
    }

    protected static HttpResponse response(
        HttpRequest req,
        String contentType,
        byte[] bytes
    ) {
        return response(req, contentType, bytes, null);
    }

    protected static HttpResponse response(
        HttpRequest req,
        String contentType,
        byte[] bytes,
        Consumer<BiConsumer<CharSequence, CharSequence>> headers
    ) {
        return new DefaultFullHttpResponse(
            HTTP_1_1,
            OK,
            Unpooled.wrappedBuffer(bytes),
            headers(req, contentType, bytes.length, headers),
            EmptyHttpHeaders.INSTANCE);
    }

    private static HttpHeaders headers(
        HttpRequest req,
        String contentType,
        int length,
        Consumer<BiConsumer<CharSequence, CharSequence>> moreHeaders
    ) {
        HttpHeaders headers = new DefaultHttpHeaders()
            .set(CONTENT_TYPE, contentType)
            .set(CONTENT_LENGTH, length)
            .set(ACCESS_CONTROL_ALLOW_HEADERS, "*");
        if (HttpUtil.isKeepAlive(req)) {
            headers.set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        if (moreHeaders != null) {
            moreHeaders.accept(headers::set);
        }
        return headers;
    }
}
