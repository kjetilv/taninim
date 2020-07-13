package mediaserver.http;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import mediaserver.gui.IndexPage;
import mediaserver.sessions.AccessLevel;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION;
import static io.netty.handler.codec.http.HttpHeaderNames.SET_COOKIE;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpResponseStatus.FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public final class Netty {

    public static HttpResponse respond(ChannelHandlerContext ctx, HttpResponseStatus status) {
        return respond(ctx, new DefaultHttpResponse(HTTP, status));
    }

    public static HttpResponse respond(ChannelHandlerContext ctx, HttpResponse response) {
        try {
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            return response;
        } catch (Exception e) {
            throw new IllegalStateException("Response failed: " + response, e);
        }
    }

    public static HttpResponse response(Req req, byte[] content, Headers headers) {
        return fullResponse(req, null, null, content, headers);
    }

    public static HttpResponse response(Req req, HttpResponseStatus status, byte[] content, Headers headers) {
        return fullResponse(req, null, status, content, headers);
    }

    public static HttpResponse response(Req req, String contentType, byte[] content) {
        return fullResponse(req, contentType, null, content, null);
    }

    public static HttpResponse response(
        Req req,
        String contentType,
        HttpResponseStatus status,
        byte[] content,
        Headers headers
    ) {
        return fullResponse(req, contentType, status, content, headers);
    }

    public static HttpResponse redirect(String ref) {
        return redirect(ref, null);
    }

    public static HttpResponse authCookieResponse(Req req, String cookie) {
        return fullResponse(req, null, null, null, setCookie(cookie));
    }

    public static HttpResponse unauthCookieResponse(String cookie) {
        return redirect(
            new Route("login", AccessLevel.NONE, Route.Method.GET),
            setCookie(cookie));
    }

    public static String authCookie(UUID uuid) {
        io.netty.handler.codec.http.cookie.Cookie cookie =
            new io.netty.handler.codec.http.cookie.DefaultCookie(
                IndexPage.ID_COOKIE,
                Objects.requireNonNull(uuid, "uuid").toString());
        cookie.setMaxAge(COOKIE_TIME);
        return ServerCookieEncoder.STRICT.encode(cookie);
    }

    public static String unauthCookie() {
        io.netty.handler.codec.http.cookie.Cookie cookie = new DefaultCookie(IndexPage.ID_COOKIE, "");
        cookie.setMaxAge(0);
        return ServerCookieEncoder.STRICT.encode(cookie);
    }

    public static HttpResponse redirect(Route location) {
        return redirect(location, null);
    }

    private Netty() {
    }

    private static final HttpVersion HTTP = HTTP_1_1;

    private static final long COOKIE_TIME = Duration.ofDays(1).toSeconds();

    private static HttpResponse redirect(Route page, Headers moreHeaders) {
        return redirect(page.getPrefix(), moreHeaders);
    }

    private static HttpResponse redirect(String ref, Headers moreHeaders) {
        HttpHeaders headers = new DefaultHttpHeaders();
        if (moreHeaders != null) {
            moreHeaders.accept(headers::set);
        }
        return new DefaultFullHttpResponse(
            HTTP, FOUND, Unpooled.EMPTY_BUFFER, headers.set(LOCATION, ref), EmptyHttpHeaders.INSTANCE);
    }

    private static HttpResponse fullResponse(
        Req req,
        String contentType,
        HttpResponseStatus status,
        byte[] content,
        Headers headers
    ) {
        HttpHeaders httpHeaders = new DefaultHttpHeaders();
        if (contentType != null) {
            httpHeaders.set(CONTENT_TYPE, contentType);
        } else {
            Optional.ofNullable(req).flatMap(Req::getResponseContentType).ifPresent(type ->
                httpHeaders.set(CONTENT_TYPE, type));
        }
        if (content != null) {
            httpHeaders.set(CONTENT_LENGTH, content.length);
        }
        if (req != null && HttpUtil.isKeepAlive(req.getRequest())) {
            httpHeaders.set(CONNECTION, KEEP_ALIVE);
        }
        if (headers != null) {
            headers.accept(httpHeaders::set);
        }
        ByteBuf body = content == null
            ? Unpooled.EMPTY_BUFFER
            : Unpooled.wrappedBuffer(content);
        return new DefaultFullHttpResponse(
            HTTP,
            status == null ? OK : status,
            body,
            httpHeaders,
            EmptyHttpHeaders.INSTANCE);
    }

    private static Headers setCookie(String cookie) {
        return headers ->
            headers.set(SET_COOKIE, cookie);
    }
}
