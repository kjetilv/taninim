package mediaserver.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import mediaserver.gui.GUI;
import mediaserver.sessions.Session;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public final class Netty {

    public static final HttpVersion HTTP = HTTP_1_1;

    private static final long COOKIE_TIME = Duration.ofDays(1).toSeconds();

    private Netty() {

    }

    public static HttpResponse respond(ChannelHandlerContext ctx, HttpResponseStatus status) {

        return respond(ctx, response(status));
    }

    public static HttpResponse response(HttpResponseStatus status) {

        return response(null, status, null, null);
    }

    public static HttpResponse respond(ChannelHandlerContext ctx, HttpResponse response) {

        try {
            ctx.writeAndFlush(response)
                .addListener(ChannelFutureListener.CLOSE);
            return response;
        } catch (Exception e) {
            throw new IllegalStateException("Response failed: " + response, e);
        }
    }

    public static HttpResponse response(WebPath webPath, byte[] content) {

        return response(webPath, content, null);
    }

    public static HttpResponse response(
        WebPath webPath,
        byte[] content,
        Consumer<BiConsumer<CharSequence, CharSequence>> moreHeaders
    ) {

        return response(webPath, null, null, content, moreHeaders);
    }

    public static HttpResponse response(
        WebPath webPath,
        HttpResponseStatus status,
        byte[] content,
        Consumer<BiConsumer<CharSequence, CharSequence>> moreHeaders
    ) {

        return response(webPath, null, status, content, moreHeaders);
    }

    public static HttpResponse response(
        WebPath webPath,
        String contentType,
        byte[] content,
        Consumer<BiConsumer<CharSequence, CharSequence>> moreHeaders
    ) {

        return response(webPath, contentType, null, content, moreHeaders);
    }

    public static HttpResponse response(
        WebPath webPath,
        String contentType,
        byte[] content
    ) {

        return response(webPath, contentType, null, content);
    }

    public static HttpResponse response(
        WebPath webPath,
        String contentType,
        HttpResponseStatus status,
        byte[] content
    ) {

        return response(webPath, contentType, status, content, null);
    }

    public static HttpResponse response(
        WebPath webPath,
        String contentType,
        HttpResponseStatus status,
        byte[] content,
        Consumer<BiConsumer<CharSequence, CharSequence>> moreHeaders
    ) {

        HttpHeaders headers = new DefaultHttpHeaders();
        if (contentType != null) {
            headers.set(CONTENT_TYPE, content);
        } else if (webPath != null) {
            headers.set(CONTENT_TYPE, webPath.getContentType());
        }
        if (content != null) {
            headers.set(CONTENT_LENGTH, content.length);
        }
        if (webPath != null && HttpUtil.isKeepAlive(webPath.getReq())) {
            headers.set(CONNECTION, KEEP_ALIVE);
        }
        if (moreHeaders != null) {
            moreHeaders.accept(headers::set);
        }
        ByteBuf body = content == null
            ? Unpooled.EMPTY_BUFFER
            : Unpooled.wrappedBuffer(content);

        return new DefaultFullHttpResponse(
            HTTP,
            status == null ? OK : status,
            body,
            headers,
            EmptyHttpHeaders.INSTANCE);
    }

    public static HttpResponse redirectResponse(
        String location,
        Consumer<BiConsumer<CharSequence, CharSequence>> moreHeaders
    ) {

        HttpHeaders headers = new DefaultHttpHeaders();
        if (moreHeaders != null) {
            moreHeaders.accept(headers::set);
        }
        return new DefaultFullHttpResponse(
            HTTP,
            HttpResponseStatus.FOUND,
            Unpooled.EMPTY_BUFFER,
            headers.set(LOCATION, location),
            EmptyHttpHeaders.INSTANCE);
    }

    public static HttpResponse authCookieResponse(WebPath webPath, String cookie) {

        return ok(webPath, null, setCookie(cookie));
    }

    public static HttpResponse unauthCookieResponse(String cookie) {

        return redirectResponse(Prefix.LOGIN.getPref(), setCookie(cookie));
    }

    public static String authCookie(Session session) {

        io.netty.handler.codec.http.cookie.Cookie cookie = new io.netty.handler.codec.http.cookie.DefaultCookie(
            GUI.ID_COOKIE, session.getCookie().toString());
        cookie.setMaxAge(COOKIE_TIME);
        return ServerCookieEncoder.STRICT.encode(cookie);
    }

    public static String unauthCookie() {

        io.netty.handler.codec.http.cookie.Cookie cookie = new DefaultCookie(GUI.ID_COOKIE, "");
        cookie.setMaxAge(0);
        return ServerCookieEncoder.STRICT.encode(cookie);
    }

    protected static HttpResponse ok(
        WebPath webPath,
        byte[] content,
        Consumer<BiConsumer<CharSequence, CharSequence>> moreHeaders
    ) {

        return response(webPath, null, null, content, moreHeaders);
    }

    protected static HttpResponse redirectResponse(String location) {

        return redirectResponse(location, null);
    }

    protected static HttpResponse okCookieResponse(WebPath webPath, String cookieCookie) {

        return ok(webPath, null, setCookie(cookieCookie));
    }

    protected static String newCookieCookie() {

        Cookie cookie = new DefaultCookie(GUI.COOKIE_COOKIE, "cakes");
        cookie.setMaxAge(Long.MAX_VALUE);
        return ServerCookieEncoder.STRICT.encode(cookie);
    }

    private static byte[] helloContent(Session session) {

        return session.getFacebookUser().getName().getBytes(StandardCharsets.UTF_8);
    }

    private static Consumer<BiConsumer<CharSequence, CharSequence>> setCookie(String cookie) {

        return headers -> headers.accept(SET_COOKIE, cookie);
    }
}
