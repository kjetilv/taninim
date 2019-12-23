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
import mediaserver.sessions.Session;
import mediaserver.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpResponseStatus.FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public abstract class NettyHandler {

    public static final HttpVersion HTTP = HTTP_1_1;

    private static final Logger log = LoggerFactory.getLogger(NettyHandler.class);

    private final Collection<Prefix> handled;

    private final WebCache<String, String> cache;

    private static final long COOKIE_TIME = Duration.ofDays(1).toSeconds();

    protected NettyHandler(Prefix... handled) {

        this.handled = new HashSet<>(Arrays.asList(handled));
        this.cache = new WebCache<>(IO::read);
    }

    public boolean couldHandle(WebPath webPath) {

        return handled.isEmpty() || handled.stream().anyMatch(webPath::hasPrefix);
    }

    public Handling handle(
        FullHttpRequest req,
        WebPath webPath,
        ChannelHandlerContext ctx
    ) {

        Handling handling = handleRequest(req, webPath, ctx);
        log(req, handling);
        return handling;
    }

    public static Handling respond(ChannelHandlerContext ctx, HttpResponseStatus status) {

        return respond(ctx, response(null, status, null, null, null));
    }

    public static Optional<UUID> authenticationId(HttpRequest req) {

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

    public static Handling respond(ChannelHandlerContext ctx, HttpResponse response) {

        try {
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            return Handling.sentResponse(response);
        } catch (Exception e) {
            throw new IllegalStateException("Response failed: " + response, e);
        }
    }

    protected abstract Handling handleRequest(
        FullHttpRequest req,
        WebPath webPath,
        ChannelHandlerContext ctx
    );

    protected String resource(WebPath path) {

        return path.getUri();
    }

    protected Template template(String resource) {

        return cache.get(resource)
            .map(source ->
                new Template(resource, source))
            .orElseThrow(() ->
                new IllegalArgumentException("No such template resource: " + resource));
    }

    protected static HttpResponse ok(
        HttpRequest req,
        byte[] content,
        Consumer<BiConsumer<CharSequence, CharSequence>> moreHeaders
    ) {

        return response(req, OK, APPLICATION_JSON.toString(), content, moreHeaders);
    }

    protected static HttpResponse redirectResponse(
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
            HTTP,
            status == null ? OK : status,
            body,
            headers,
            EmptyHttpHeaders.INSTANCE);
    }

    protected static HttpResponse okCookieResponse(HttpRequest req, String cookieCookie) {

        return ok(req, null, setCookie(cookieCookie));
    }

    protected static HttpResponse authCookieResponse(HttpRequest req, Session session, String cookie) {

        return ok(req, helloContent(session), setCookie(cookie));
    }

    protected static HttpResponse unauthCookieResponse(HttpRequest req, String cookie) {

        return redirectResponse("/", setCookie(cookie));
    }

    protected static String authCookie(Session session) {

        Cookie cookie = new DefaultCookie(GUI.ID_COOKIE, session.getCookie().toString());
        cookie.setMaxAge(COOKIE_TIME);
        return ServerCookieEncoder.STRICT.encode(cookie);
    }

    protected static String unauthCookie() {

        Cookie cookie = new DefaultCookie(GUI.ID_COOKIE, "");
        cookie.setMaxAge(0);
        return ServerCookieEncoder.STRICT.encode(cookie);
    }

    protected static String newCookieCookie() {

        Cookie cookie = new DefaultCookie(GUI.COOKIE_COOKIE, "cakes");
        cookie.setMaxAge(Long.MAX_VALUE);
        return ServerCookieEncoder.STRICT.encode(cookie);
    }

    protected Handling respondBytes(
        FullHttpRequest req,
        WebPath webPath,
        ChannelHandlerContext ctx,
        byte[] bytes
    ) {

        try {
            HttpResponse response =
                response(req, null, webPath.contentType(), bytes, null);
            return respond(ctx, response);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to respond to " + webPath, e);
        }
    }

    protected Function<byte[], Handling> respondBytes(
        FullHttpRequest req,
        WebPath webPath,
        ChannelHandlerContext ctx
    ) {

        return bytes ->
            respondBytes(req, webPath, ctx, bytes);
    }

    private void log(FullHttpRequest req, Handling handling) {

        if (handling.isPass()) {
            log.debug("Skipped by {}: {}", this, req.uri());
        } else {
            log.debug("Handled by {}: {} => {}", this, req.uri(), handling.getSentResponse().status());
        }
    }

    private static byte[] helloContent(Session session) {

        return String.format("\"%s\"", session.getFacebookUser().getName()).getBytes(StandardCharsets.UTF_8);
    }

    private static Consumer<BiConsumer<CharSequence, CharSequence>> setCookie(String cookie) {

        return headers -> headers.accept(SET_COOKIE, cookie);
    }
}
