package mediaserver.gui;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.util.AsciiString;
import mediaserver.sessions.Session;
import mediaserver.util.IO;
import mediaserver.util.URLs;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
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

    public static HttpResponse respond(ChannelHandlerContext ctx, String path, HttpResponseStatus status) {

        return respond(ctx, path, response(null, status, (String)null, null, null));
    }

    public static Optional<UUID> authCookie(HttpRequest req) {

        return Optional.of(req.headers())
            .map(headers ->
                headers.get(COOKIE))
            .map(ServerCookieDecoder.STRICT::decode)
            .stream()
            .flatMap(Collection::stream)
            .filter(cookie ->
                cookie.name().equalsIgnoreCase(GUI.TANINIM_ID))
            .map(Cookie::value)
            .map(UUID::fromString)
            .findFirst();
    }

    public HttpResponse handle(FullHttpRequest req, String path, ChannelHandlerContext ctx) {

        return respond(ctx, path, BAD_REQUEST);
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
        HttpResponseStatus status,
        AsciiString contentType,
        Consumer<BiConsumer<CharSequence, CharSequence>> moreHeaders
    ) {

        return response(req, status, contentType, null, moreHeaders);
    }

    static HttpResponse response(
        HttpRequest req,
        HttpResponseStatus status,
        AsciiString contentType,
        byte[] content,
        Consumer<BiConsumer<CharSequence, CharSequence>> moreHeaders
    ) {
        return response(
            req, status, contentType == null ? null : contentType.toString(), content, moreHeaders);
    }

    static HttpResponse response(
        HttpRequest req,
        HttpResponseStatus status,
        String contentType,
        byte[] content,
        Consumer<BiConsumer<CharSequence, CharSequence>> moreHeaders
    ) {

        HttpHeaders headers = new DefaultHttpHeaders();
        if (contentType != null) {
            headers.set(CONTENT_TYPE, contentType);
        }
        if (content != null) {
            headers.set(CONTENT_LENGTH, content.length);
        }
        if (req != null && HttpUtil.isKeepAlive(req)) {
            headers.set(CONNECTION, KEEP_ALIVE);
        }
        if (moreHeaders != null) {
            moreHeaders.accept(headers::set);
        }
        ByteBuf body = content == null
            ? Unpooled.EMPTY_BUFFER
            : Unpooled.wrappedBuffer(content);

        return new DefaultFullHttpResponse(
            HTTP_1_1, status == null ? OK : status, body, headers, EmptyHttpHeaders.INSTANCE);
    }

    protected static QPars qpars(String uri) {

        return new QPars(params(uri, uri.indexOf("?")));
    }

    protected String uriPath(String uri) {

        int queryIndex = uri.indexOf("?");
        return queryIndex < 0 ? uri : uri.substring(0, queryIndex);
    }

    protected static HttpResponse okCookieResponse(HttpRequest req, String cookieCookie) {

        return response(req, OK, APPLICATION_JSON, setCookie(cookieCookie));
    }

    protected static HttpResponse helloCookieResponse(HttpRequest req, Session session, String cookie) {

        return response(req, OK, APPLICATION_JSON, helloContent(session), setCookie(cookie));
    }

    protected static String cookie(Session session) {

        Cookie cookie = new io.netty.handler.codec.http.cookie.DefaultCookie(
            GUI.TANINIM_ID, session == null ? "" : session.getCookie().toString());
        cookie.setMaxAge(COOKIE_TIME);
        return ServerCookieEncoder.STRICT.encode(cookie);
    }

    protected static String cookieCookie() {

        Cookie cookie = new io.netty.handler.codec.http.cookie.DefaultCookie(GUI.COOKIES_OK, "gimmeCookies");
        cookie.setMaxAge(Long.MAX_VALUE);
        return ServerCookieEncoder.STRICT.encode(cookie);
    }

    private static byte[] helloContent(Session session) {

        return jsonString(session.getFacebookUser().getName()).getBytes(StandardCharsets.UTF_8);
    }

    private static Consumer<BiConsumer<CharSequence, CharSequence>> setCookie(String cookie) {

        return headers ->
            headers.accept(SET_COOKIE, cookie);
    }

    private static String jsonString(String name1) {

        return "\"" + name1 + "\"";
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

    private static Map<QPar, String> params(String uri, int queryIndex) {

        return queryIndex < 0
            ? Collections.emptyMap()
            : URLs.queryParams(uri.substring(queryIndex + 1));
    }
}
