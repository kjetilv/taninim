package mediaserver.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.util.AsciiString;
import mediaserver.gui.GUI;
import mediaserver.sessions.AccessLevel;
import mediaserver.sessions.Session;
import mediaserver.util.MostlyOnce;
import mediaserver.util.URLs;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.netty.handler.codec.http.HttpHeaderNames.COOKIE;
import static io.netty.handler.codec.http.HttpHeaderNames.HOST;

public final class WebPath {

    public static final String AUDIO_FLAC = "audio/flac";

    public static final String AUDIO_AAC = "audio/m4a";

    private final ChannelHandlerContext ctx;

    private final Page page;

    private final String path;

    private final String uri;

    private final Session session;

    private final Instant time;

    private final Supplier<String> host;

    private final Supplier<Optional<UUID>> uuid;

    private final Supplier<QPars> qpars;

    private final AtomicReference<Boolean> keepAlive = new AtomicReference<>();

    private final Supplier<Optional<String>> content;

    private Supplier<Optional<String>> responseContentType;

    private final FullHttpRequest request;

    private final Supplier<ConcurrentHashMap<String, Optional<String>>> headers =
        MostlyOnce.get(ConcurrentHashMap::new);

    private WebPath(
        ChannelHandlerContext ctx,
        Page page,
        String uri,
        FullHttpRequest request,
        Session session,
        Instant time
    ) {

        this.ctx = ctx;

        this.page = Objects.requireNonNull(page, "page");
        this.uri = Objects.requireNonNull(uri, "uri");
        this.request = Objects.requireNonNull(request, "req");
        this.session = session;
        this.time = Objects.requireNonNull(time, "time");

        int pathIndex = this.uri.indexOf("?");
        this.path = pathIndex > 0 ? this.uri.substring(0, pathIndex) : this.uri;

        this.responseContentType = MostlyOnce.get(this::contentType);
        this.uuid = MostlyOnce.get(() -> authentication(this.request));
        this.qpars = MostlyOnce.get(() -> new QPars(params(this.uri, this.uri.indexOf("?"))));
        this.content = MostlyOnce.get(() ->
            Optional.of(this.request.content())
                .map(content ->
                    content.toString(StandardCharsets.UTF_8)));
        this.host = MostlyOnce.get(() ->
            Optional.ofNullable(this.request.headers().getAsString(HOST))
                .orElse("localhost"));
    }

    public WebPath with(Session session) {

        return new WebPath(ctx, page, uri, request, Objects.requireNonNull(session, "session"), time);
    }

    public Page getPage() {

        return page;
    }

    public String getPath() {

        return getPath(false);
    }

    public String getPath(boolean unslash) {

        String p = path;
        while (unslash && p.startsWith("/")) {
            p = p.substring(1);
        }
        return p;
    }

    public ChannelHandlerContext getCtx() {

        return ctx;
    }

    public Instant getTime() {

        return time;
    }

    public String getUri() {

        return uri;
    }

    public String getHost() {

        return host.get();
    }

    public Session getSession() {

        return session;
    }

    public String header(AsciiString header) {

        return header(header.toString());
    }

    public String header(String header) {

        return headers.get()
            .computeIfAbsent(header, __ ->
                Optional.ofNullable(request.headers().getAsString(header)))
            .orElse(null);
    }

    public Optional<String> getResponseContentType() {

        return responseContentType.get();
    }

    public Optional<UUID> get(QPar queryParameter) {

        return getQueryParameters().apply(queryParameter);
    }

    public QPars getQueryParameters() {

        return qpars.get();
    }

    public Optional<String> getContent() {

        return content.get();
    }

    public boolean isFor(Page page) {

        return this.page == page;
    }

    public Optional<UUID> getAuthentication() {

        return uuid.get();
    }

    public FullHttpRequest getRequest() {

        return request;
    }

    public static Stream<WebPath> from(ChannelHandlerContext ctx, FullHttpRequest request, Instant time) {

        return Stream.of(request)
            .map(HttpRequest::uri)
            .filter(uri ->
                !uri.isBlank())
            .map(uri ->
                URLDecoder.decode(uri, StandardCharsets.UTF_8))
            .flatMap(uri ->
                Page.get(uri).map(page ->
                    new WebPath(ctx, page, page.resolve(uri), request, null, time)));
    }

    public boolean isKeepAlive() {

        return keepAlive.updateAndGet(value -> value == null
            ? HttpUtil.isKeepAlive(request)
            : value);
    }

    public boolean isFlac() {

        return request.uri().endsWith(".flac");
    }

    public AccessLevel getAccessLevel() {

        return session == null ? AccessLevel.NONE : session.getAccessLevel();
    }

    private Optional<String> contentType() {

        return Optional.ofNullable(path.endsWith(".css") ? "text/css"
            : path.endsWith(".js") ? "text/javascript"
            : path.endsWith(".ico") ? "image/x-icon"
            : null);
    }

    private static Map<QPar, String> params(String uri, int queryIndex) {

        return queryIndex < 0
            ? Collections.emptyMap()
            : URLs.queryParams(uri.substring(queryIndex + 1));
    }

    private Optional<UUID> authentication(HttpRequest req) {

        return cookies(req)
            .findFirst()
            .or(() ->
                get(QPar.STREAMLEASE));
    }

    private Stream<UUID> cookies(HttpRequest req) {

        return Optional.of(req.headers())
            .map(headers ->
                headers.get(COOKIE))
            .map(ServerCookieDecoder.STRICT::decode)
            .stream()
            .flatMap(Collection::stream)
            .filter(cookie ->
                cookie.name().equalsIgnoreCase(GUI.ID_COOKIE))
            .map(cookie ->
                UUID.fromString(cookie.value()));
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + page.getPref() + path + "]";
    }
}
