package mediaserver.gui;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import mediaserver.util.IO;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public abstract class Nettish {

    private final IO io;

    private final List<String> prefix;

    protected static final Consumer<BiConsumer<CharSequence, CharSequence>> IMMUTABLE = headers ->
        headers.accept(CACHE_CONTROL, "immutable");

    Nettish(IO io, String... prefix) {
        this.io = io;
        this.prefix = List.of(prefix);
    }

    public boolean shouldHandle(String path) {
        return matching(path).isPresent();
    }

    public String getPrefix(String path) {
        return matching(path).orElseThrow(() ->
            new IllegalArgumentException("Unsupported: " + path + " != " + prefix));
    }

    public static HttpResponse redirect(ChannelHandlerContext ctx, String value) {
        return respond(
            ctx,
            value,
            new DefaultFullHttpResponse(
                HTTP_1_1,
                HttpResponseStatus.FOUND,
                Unpooled.buffer(0),
                new DefaultHttpHeaders()
                    .set(LOCATION, value),
                EmptyHttpHeaders.INSTANCE));
    }

    public static HttpResponse respond(ChannelHandlerContext ctx, String path, HttpResponseStatus response) {
        return respond(ctx, path, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, response));
    }

    public abstract HttpResponse handle(HttpRequest req, String path, ChannelHandlerContext ctx);

    String resource(String path) {
        return path.substring(getPrefix(path).length());
    }

    Template template(String resource) {
        return new Template(io, resource);
    }

    Optional<byte[]> readBytes(String resource) {
        return io.readBytes(resource);
    }

    URL resolve(String path) {
        return io.resolve(path);
    }

    static HttpResponse respond(ChannelHandlerContext ctx, String path, HttpResponse response) {
        try {
            ctx.writeAndFlush(response)
                .addListener(ChannelFutureListener.CLOSE);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to respond to " + path + ": " + response, e);
        }
        return response;
    }

    static HttpResponse response(
        HttpRequest req,
        String contentType,
        byte[] bytes
    ) {
        return response(req, contentType, bytes, null);
    }

    static HttpResponse response(
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

    private Optional<String> matching(String path) {
        return prefix.stream().filter(path::startsWith).findFirst();
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
