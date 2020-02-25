package mediaserver.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import mediaserver.gui.IndexPage;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpResponseStatus.FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public final class Netty {

    private static final HttpVersion HTTP = HTTP_1_1;

    private static final long COOKIE_TIME = Duration.ofDays(1).toSeconds();

    private Netty() {

    }

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

    public static HttpResponse response(Req req, byte[] content, Headers moreHeaders) {

        return fullResponse(req, null, null, content, moreHeaders);
    }

    public static HttpResponse response(Req req, HttpResponseStatus status, byte[] content, Headers moreHeaders) {

        return fullResponse(req, null, status, content, moreHeaders);
    }

    public static HttpResponse response(Req req, String contentType, byte[] content) {

        return response(req, contentType, null, content);
    }

    private static HttpResponse response(Req req, String contentType, HttpResponseStatus status, byte[] content) {

        return fullResponse(req, contentType, status, content, null);
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

    private static HttpResponse redirect(Route page, Headers moreHeaders) {

        return redirect(page.getPref(), moreHeaders);
    }

    public static HttpResponse redirect(String ref) {

        return redirect(ref, null);
    }

    private static HttpResponse redirect(String ref, Headers moreHeaders) {

        HttpHeaders headers = new DefaultHttpHeaders();
        if (moreHeaders != null) {
            moreHeaders.accept(headers::set);
        }
        return new DefaultFullHttpResponse(
            HTTP, FOUND, Unpooled.EMPTY_BUFFER, headers.set(LOCATION, ref), EmptyHttpHeaders.INSTANCE);
    }

    public static HttpResponse authCookieResponse(Req req, String cookie) {

        return ok(req, null, setCookie(cookie));
    }

    public static HttpResponse unauthCookieResponse(String cookie) {

        return redirect(Route.LOGIN, setCookie(cookie));
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

    private static HttpResponse ok(Req req, byte[] content, Headers moreHeaders) {

        return fullResponse(req, null, null, content, moreHeaders);
    }

    private static HttpResponse fullResponse(
        Req req,
        String contentType,
        HttpResponseStatus status,
        byte[] content,
        Headers moreHeaders
    ) {

        HttpHeaders headers = new DefaultHttpHeaders();
        if (contentType != null) {
            headers.set(CONTENT_TYPE, contentType);
        } else {
            Optional.ofNullable(req).flatMap(Req::getResponseContentType).ifPresent(type ->
                headers.set(CONTENT_TYPE, type));
        }
        if (content != null) {
            headers.set(CONTENT_LENGTH, content.length);
        }
        if (req != null && HttpUtil.isKeepAlive(req.getRequest())) {
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

    private static Headers setCookie(String cookie) {

        return headers -> headers.accept(SET_COOKIE, cookie);
    }
}
