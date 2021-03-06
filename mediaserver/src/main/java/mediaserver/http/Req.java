package mediaserver.http;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.MixedAttribute;
import io.netty.util.AsciiString;
import mediaserver.Config;
import mediaserver.gui.IndexPage;
import mediaserver.sessions.AccessLevel;
import mediaserver.sessions.Session;
import mediaserver.sessions.User;
import mediaserver.util.MostlyOnce;

import static io.netty.handler.codec.http.HttpHeaderNames.COOKIE;
import static io.netty.handler.codec.http.HttpHeaderNames.HOST;

public final class Req {

    public static Optional<Req> from(
        Route route,
        ChannelHandlerContext ctx,
        FullHttpRequest request,
        Instant time
    ) {
        return Stream.of(request)
            .map(HttpRequest::uri)
            .filter(uri ->
                !uri.isBlank())
            .map(uri ->
                URLDecoder.decode(uri, StandardCharsets.UTF_8))
            .map(uri ->
                new Req(ctx, request, route, route.resolve(uri), time, null))
            .findFirst();
    }

    private final ChannelHandlerContext ctx;

    private final Route route;

    private final String path;

    private final String uri;

    private final Session session;

    private final Instant time;

    private final Supplier<String> host;

    private final Supplier<Optional<UUID>> uuid;

    private final Supplier<Pars<QPar, Req, String>> qpars;

    private final Supplier<Pars<FPar, Req, String>> fpars;

    private final Supplier<Boolean> keepAlive;

    private final Supplier<String> content;

    private final Supplier<String> responseContentType;

    private final FullHttpRequest request;

    private final Supplier<ConcurrentHashMap<String, Optional<String>>> headers =
        MostlyOnce.get(ConcurrentHashMap::new);

    private final Supplier<String> formattedTime;

    private final Supplier<Boolean> local;

    private Req(
        ChannelHandlerContext ctx,
        FullHttpRequest request,
        Route route,
        String uri,
        Instant time,
        Session session
    ) {
        this.ctx = Objects.requireNonNull(ctx, "ctx");
        this.route = Objects.requireNonNull(route, "page");
        this.uri = Objects.requireNonNull(uri, "uri");
        this.request = Objects.requireNonNull(request, "req");
        this.time = Objects.requireNonNull(time, "time");
        this.session = session;

        int pathIndex = this.uri.indexOf('?');
        this.path = pathIndex > 0 ? this.uri.substring(0, pathIndex) : this.uri;
        this.responseContentType = MostlyOnce.get(this::contentType);
        this.local = MostlyOnce.get(this::computeLocal);
        this.uuid = MostlyOnce.get(this::authentication);
        this.qpars = MostlyOnce.get(() ->
            new Pars<>(qparams(this.uri, this.uri.indexOf('?'))));
        this.fpars = MostlyOnce.get(() ->
            new Pars<>(fparams(request)));
        this.keepAlive = MostlyOnce.get(() ->
            HttpUtil.isKeepAlive(this.request));
        this.content = MostlyOnce.get(() ->
            Optional.of(this.request.content())
                .map(content ->
                    content.toString(StandardCharsets.UTF_8))
                .orElse(null));
        this.host = MostlyOnce.get(() ->
            Optional.ofNullable(this.request.headers().getAsString(HOST))
                .orElse("localhost"));
        this.formattedTime = MostlyOnce.get(() ->
            this.time.atZone(Config.TIMEZONE).format(DateTimeFormatter.ISO_TIME));
    }

    public boolean hasAccess(AccessLevel accessLevel) {
        return getAccessLevel().satisfies(accessLevel);
    }

    public boolean hasRoute() {
        return route.accessibleBy(request.method().toString()) && route.accessibleIn(session);
    }

    public Req boundTo(Session session) {
        try {
            return new Req(
                ctx,
                request,
                route,
                uri,
                time, Objects.requireNonNull(session, "session")
            );
        } finally {
            session.setLastAccessed(this);
        }
    }

    public boolean isPost() {
        return request.method().equals(HttpMethod.POST);
    }

    public boolean isBound() {
        return session != null;
    }

    public Route getRoute() {
        return route;
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
                Optional.ofNullable(request.headers().get(header)))
            .orElse(null);
    }

    public Optional<String> getContent() {
        return Optional.ofNullable(content.get());
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

    public boolean isLocal() {
        return session != null && session.isLocal() || local.get();
    }

    Pars<QPar, Req, String> getQueryParameters() {
        return qpars.get();
    }

    Pars<FPar, Req, String> getFormParameters() {
        return fpars.get();
    }

    Optional<String> getResponseContentType() {
        return Optional.ofNullable(responseContentType.get());
    }

    boolean isFor(Route page) {
        return this.route.equals(page);
    }

    private boolean computeLocal() {
        return get(Channel::localAddress)
            .map(sameSubnet ->
                "localhost".equals(sameSubnet.getHostName()) || get(Channel::remoteAddress)
                    .map(remoteAddr ->
                        sameSubnet(sameSubnet, remoteAddr))
                    .orElse(false))
            .orElse(false);
    }

    private Optional<InetSocketAddress> get(Function<Channel, SocketAddress> remoteAddress) {
        return Optional.ofNullable(ctx.channel())
            .map(remoteAddress)
            .filter(InetSocketAddress.class::isInstance)
            .map(InetSocketAddress.class::cast);
    }

    private String contentType() {
        return path.endsWith(".css") ? "text/css"
            : path.endsWith(".js") ? "text/javascript"
                : path.endsWith(".ico") ? "image/x-icon"
                    : null;
    }

    private Optional<UUID> authentication() {
        return Stream.concat(cookieUUID(request), QPar.streamlease.id(this)).findFirst();
    }

    private static boolean sameSubnet(InetSocketAddress a1, InetSocketAddress a2) {
        byte[] b1 = a1.getAddress().getAddress();
        byte[] b2 = a2.getAddress().getAddress();
        return IntStream.range(0, b1.length - 1).allMatch(i -> b1[i] == b2[i]);
    }

    private static Map<FPar, Collection<String>> fparams(HttpRequest request) {
        return new HttpPostRequestDecoder(request).getBodyHttpDatas().stream()
            .filter(MixedAttribute.class::isInstance)
            .map(MixedAttribute.class::cast)
            .collect(Collectors.toMap(
                attr ->
                    FPar.valueOf(attr.getName()),
                attr ->
                    Collections.singleton(attr.content().toString(StandardCharsets.UTF_8))));
    }

    private static Map<QPar, Collection<String>> qparams(String uri, int queryIndex) {
        return queryIndex < 0 ? Collections.emptyMap() : URLs.queryParams(uri.substring(queryIndex + 1));
    }

    private static Stream<UUID> cookieUUID(HttpRequest req) {
        return Optional.of(req.headers())
            .map(headers ->
                headers.get(COOKIE))
            .map(ServerCookieDecoder.STRICT::decode)
            .stream()
            .flatMap(Collection::stream)
            .filter(cookie ->
                cookie.name().equalsIgnoreCase(IndexPage.ID_COOKIE))
            .map(cookie ->
                UUID.fromString(cookie.value()));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + route.getPrefix() + path + "@" + formattedTime.get() + "]";
    }
}
