package mediaserver.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import mediaserver.gui.GUI;
import mediaserver.gui.Template;
import mediaserver.gui.WebCache;
import mediaserver.sessions.Session;
import mediaserver.util.IO;
import mediaserver.util.URLs;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public abstract class Nettish {

    private final List<String> prefix;

    private final WebCache<String, String> cache;

    private static final long COOKIE_TIME = Duration.ofDays(1).toSeconds();

    protected Nettish(String... prefix) {

        this.prefix = List.of(prefix);
        this.cache = new WebCache<>(IO::read);
    }

    public boolean shouldHandle(String path) {

        return matching(path).isPresent();
    }

    public String getPrefix(String path) {

        return matching(path).orElseThrow(() ->
            new IllegalArgumentException("Unsupported: " + path + " != " + prefix));
    }

    public static HttpResponse redirect(ChannelHandlerContext ctx, String value) {

        return respond(ctx, redirect(value));
    }

    public static HttpResponse respond(ChannelHandlerContext ctx, HttpResponseStatus status) {

        return respond(ctx, response(null, status, null, null, null));
    }

    public static Optional<UUID> authCookie(HttpRequest req) {

        return Optional.of(req.headers())
            .map(headers ->
                headers.get(COOKIE))
            .map(ServerCookieDecoder.STRICT::decode)
            .stream()
            .flatMap(Collection::stream)
            .filter(cookie ->
                cookie.name().equalsIgnoreCase(GUI.ID_COOKIE))
            .map(Cookie::value)
            .map(UUID::fromString)
            .findFirst();
    }

    public abstract Optional<HttpResponse> handle(
        FullHttpRequest req,
        String path,
        ChannelHandlerContext ctx);

    protected String resource(String path) {

        return path.substring(getPrefix(path).length());
    }

    protected Template template(String resource) {

        return cache.get(resource)
            .map(source ->
                new Template(resource, source))
            .orElseThrow(() ->
                new IllegalArgumentException("No such template resource: " + resource));
    }

    protected static HttpResponse respond(ChannelHandlerContext ctx, HttpResponse response) {

        try {
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } catch (Exception e) {
            throw new IllegalStateException("Response failed: " + response, e);
        }
        return response;
    }

    protected static HttpResponse ok(
        HttpRequest req,
        byte[] content,
        Consumer<BiConsumer<CharSequence, CharSequence>> moreHeaders
    ) {

        return response(
            req, HttpResponseStatus.OK, HttpHeaderValues.APPLICATION_JSON.toString(), content, moreHeaders);
    }

    protected static HttpResponse response(
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

        return ok(req, null, setCookie(cookieCookie));
    }

    protected static HttpResponse authCookieResponse(HttpRequest req, Session session, String cookie) {

        return ok(req, helloContent(session), setCookie(cookie));
    }

    protected static String newAuthCookie(Session session) {

        Cookie cookie = new DefaultCookie(GUI.ID_COOKIE, session == null ? "" : session.getCookie().toString());
        cookie.setMaxAge(COOKIE_TIME);
        return ServerCookieEncoder.STRICT.encode(cookie);
    }

    protected static String newCookieCookie() {

        Cookie cookie = new DefaultCookie(GUI.COOKIE_COOKIE, "cakes");
        cookie.setMaxAge(Long.MAX_VALUE);
        return ServerCookieEncoder.STRICT.encode(cookie);
    }

    private static byte[] helloContent(Session session) {

        return String.format("\"%s\"", session.getFacebookUser().getName()).getBytes(StandardCharsets.UTF_8);
    }

    private static Consumer<BiConsumer<CharSequence, CharSequence>> setCookie(String cookie) {

        return headers -> headers.accept(SET_COOKIE, cookie);
    }

    private static HttpResponse redirect(String value) {

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
