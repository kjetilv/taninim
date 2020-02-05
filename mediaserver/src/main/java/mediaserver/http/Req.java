package mediaserver.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.util.AsciiString;
import mediaserver.Config;
import mediaserver.gui.IndexPage;
import mediaserver.sessions.AccessLevel;
import mediaserver.sessions.Session;
import mediaserver.sessions.User;
import mediaserver.util.MostlyOnce;
import mediaserver.util.URLs;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.netty.handler.codec.http.HttpHeaderNames.COOKIE;
import static io.netty.handler.codec.http.HttpHeaderNames.HOST;

public final class Req {

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

    private final Supplier<Boolean> keepAlive;

    private final Supplier<Optional<String>> content;

    private final Supplier<Optional<String>> responseContentType;

    private final FullHttpRequest request;

    private final Supplier<ConcurrentHashMap<String, Optional<String>>> headers =
        MostlyOnce.get(ConcurrentHashMap::new);

    private final Supplier<String> formattedTime;

    private Req(
        ChannelHandlerContext ctx,
        FullHttpRequest request,
        Page page,
        String uri,
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
        this.uuid = MostlyOnce.get(() ->
            authentication(this.request));
        this.qpars = MostlyOnce.get(() ->
            new QPars(params(this.uri, this.uri.indexOf("?"))));
        this.keepAlive = MostlyOnce.get(() ->
            HttpUtil.isKeepAlive(this.request));
        this.content = MostlyOnce.get(() ->
            Optional.of(this.request.content())
                .map(content ->
                    content.toString(StandardCharsets.UTF_8)));
        this.host = MostlyOnce.get(() ->
            Optional.ofNullable(this.request.headers().getAsString(HOST))
                .orElse("localhost"));
        this.formattedTime = MostlyOnce.get(() ->
            this.time.atZone(Config.TIMEZONE).format(DateTimeFormatter.ISO_TIME));
    }

    public boolean isAllowed() {

        return page.accessibleBy(request.method().toString()) && page.accessibleIn(session);
    }

    public Req bind(Session session) {

        try {
            return new Req(ctx, request, page, uri, Objects.requireNonNull(session, "session"), time);
        } finally {
            session.setLastAccessed(this);
        }
    }

    public Page getPage() {

        return page;
    }

    public String getPath() {

        return getPath(false);
    }

    public String getPath(boolean unslash) {

        if (unslash) {
            String p = path;
            while (p.startsWith("/")) {
                p = p.substring(1);
            }
            return p;
        }
        return path;
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

    public Optional<UUID> getUUIDParameter(QPar queryParameter) {

        return getQueryParameters().get(queryParameter);
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

    public String getReferer() {
        return request.headers().getAsString(HttpHeaderNames.REFERER);
    }

    public static Optional<Req> from(ChannelHandlerContext ctx, FullHttpRequest request, Instant time) {

        return Stream.of(request)
            .map(HttpRequest::uri)
            .filter(uri ->
                !uri.isBlank())
            .map(uri ->
                URLDecoder.decode(uri, StandardCharsets.UTF_8))
            .flatMap(uri ->
                Page.get(uri).map(page ->
                    new Req(ctx, request, page, page.resolve(uri), null, time)))
            .findFirst();
    }

    public boolean isKeepAlive() {

        return keepAlive.get();
    }

    public boolean isFlac() {

        return request.uri().endsWith(".flac");
    }

    public AccessLevel getAccessLevel() {

        return session == null ? AccessLevel.NONE : session.getAccessLevel();
    }

    public Duration getSessionTimeLeft() {

        return Duration.between(time, session.getEndTime());
    }

    public User getActiveUser() {

        return session.getActiveUser(this);
    }

    private Optional<String> contentType() {

        return Optional.ofNullable(path.endsWith(".css") ? "text/css"
            : path.endsWith(".js") ? "text/javascript"
            : path.endsWith(".ico") ? "image/x-icon"
            : null);
    }

    private static Map<QPar, String> params(String uri, int queryIndex) {

        return queryIndex < 0 ? Collections.emptyMap() : URLs.queryParams(uri.substring(queryIndex + 1));
    }

    private Optional<UUID> authentication(HttpRequest req) {

        return cookieUUID(req).or(() -> getUUIDParameter(QPar.STREAMLEASE));
    }

    private Optional<UUID> cookieUUID(HttpRequest req) {

        return Optional.of(req.headers())
            .map(headers ->
                headers.get(COOKIE))
            .map(ServerCookieDecoder.STRICT::decode)
            .stream()
            .flatMap(Collection::stream)
            .filter(cookie ->
                cookie.name().equalsIgnoreCase(IndexPage.ID_COOKIE))
            .map(cookie ->
                UUID.fromString(cookie.value()))
            .findFirst();
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + page.getPref() + path + "@" + formattedTime.get() + "]";
    }
}
