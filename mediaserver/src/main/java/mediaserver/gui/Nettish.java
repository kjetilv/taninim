package mediaserver.gui;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import mediaserver.sessions.Session;
import mediaserver.util.IO;
import mediaserver.util.URLs;

import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public abstract class Nettish {

    private final IO io;

    private final List<String> prefix;

    protected static final Consumer<BiConsumer<CharSequence, CharSequence>> IMMUTABLE =
        headers ->
            headers.accept(CACHE_CONTROL, "immutable");

    private static final long COOKIE_TIME = Duration.ofDays(1).toSeconds();

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

        return respond(ctx, value, redirect(value));
    }

    public static HttpResponse respond(ChannelHandlerContext ctx, String path, HttpResponseStatus response) {

        return respond(ctx, path, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, response));
    }

    public HttpResponse handle(FullHttpRequest req, String path, ChannelHandlerContext ctx) {

        return respond(ctx, path, BAD_REQUEST);
    }

    public static Optional<UUID> authCookie(HttpRequest req) {

        Collection<Cookie> cookies = ServerCookieDecoder.STRICT.decode(req.headers().get(COOKIE));
        return cookies.stream()
            .filter(cookie ->
                cookie.name().equalsIgnoreCase(GUI.TANINIM_ID))
            .map(Cookie::value)
            .map(UUID::fromString)
            .findFirst();
    }

    public static Optional<UUID> authToken(HttpRequest req) {

        return qpars(req.uri()).apply(QPar.USER);
    }

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

    protected static QPars qpars(String uri) {

        return new QPars(params(uri, uri.indexOf("?")));
    }

    protected String uriPath(String uri) {

        int queryIndex = uri.indexOf("?");
        return queryIndex < 0 ? uri : uri.substring(0, queryIndex);
    }

    protected static HttpResponse cookieResponse(String cookie) {

        return new DefaultFullHttpResponse(
            HTTP_1_1,
            OK,
            Unpooled.EMPTY_BUFFER,
            new DefaultHttpHeaders()
                .set(SET_COOKIE, cookie),
            EmptyHttpHeaders.INSTANCE);
    }

    protected static String cookie(Session session) {

        Cookie cookie = new io.netty.handler.codec.http.cookie.DefaultCookie(GUI.TANINIM_ID, session == null ? "" : session.getCookie().toString());
        cookie.setMaxAge(COOKIE_TIME);
        return ServerCookieEncoder.STRICT.encode(cookie);
    }

    private static DefaultFullHttpResponse redirect(String value) {

        return new DefaultFullHttpResponse(
            HTTP_1_1,
            HttpResponseStatus.FOUND,
            Unpooled.EMPTY_BUFFER,
            new DefaultHttpHeaders().set(LOCATION, value),
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

    private static Map<QPar, String> params(String uri, int queryIndex) {

        return queryIndex < 0
            ? Collections.emptyMap()
            : URLs.queryParams(uri.substring(queryIndex + 1));
    }
}
