package mediaserver.http;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.util.AsciiString;
import mediaserver.gui.GUI;
import mediaserver.util.MostlyOnce;
import mediaserver.util.URLs;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static io.netty.handler.codec.http.HttpHeaderNames.COOKIE;
import static io.netty.handler.codec.http.HttpHeaderNames.HOST;

public final class WebPath {

    private final Prefix prefix;

    private final String path;

    private final String uri;

    private final Supplier<String> host;

    private final Supplier<Optional<UUID>> uuid;

    private final Supplier<QPars> qpars;

    private final Supplier<Optional<String>> content;

    private Supplier<String> contentType;

    private final FullHttpRequest req;

    private final Supplier<ConcurrentHashMap<String, Optional<String>>> headers =
        MostlyOnce.get(ConcurrentHashMap::new);

    public WebPath(Prefix prefix, String uri, FullHttpRequest req) {

        this.req = Objects.requireNonNull(req, "req");
        this.prefix = Objects.requireNonNull(prefix, "prefix");
        this.uri = this.prefix.resolve(Objects.requireNonNull(uri, "uri"));

        int pathIndex = this.uri.indexOf("?");
        this.path = pathIndex > 0 ? this.uri.substring(0, pathIndex) : this.uri;

        this.contentType = MostlyOnce.get(() ->
            path.endsWith(".css") ? "text/css"
                : path.endsWith(".js") ? "text/javascript"
                : path.endsWith(".ico") ? "image/x-icon"
                : path.endsWith(".flac") ? "audio/flac"
                : path.endsWith("m4a") ? "audio/aac"
                : "text/plain");
        this.uuid = MostlyOnce.get(() ->
            authentication(this.req));
        this.qpars = MostlyOnce.get(() ->
            new QPars(params(this.uri, this.uri.indexOf("?"))));
        this.content = MostlyOnce.get(() ->
            Optional.of(this.req.content())
                .map(content ->
                    content.toString(StandardCharsets.UTF_8)));
        this.host = MostlyOnce.get(() ->
            Optional.ofNullable(this.req.headers().getAsString(HOST))
                .orElse("localhost"));
    }

    public Prefix getPrefix() {

        return prefix;
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

    public String getUri() {

        return uri;
    }

    public String getHost() {

        return host.get();
    }

    public String header(AsciiString header) {

        return header(header.toString());
    }

    public String header(String header) {

        return headers.get()
            .computeIfAbsent(header, __ ->
                Optional.ofNullable(req.headers().getAsString(header)))
            .orElse(null);
    }

    public String getContentType() {

        return contentType.get();
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

    public boolean hasPrefix(Prefix prefix) {

        return this.prefix == prefix;
    }

    public boolean requiresAuthentication() {

        return prefix.isAuthenticated();
    }

    public Optional<UUID> getAuthentication() {

        return uuid.get();
    }

    public FullHttpRequest getReq() {

        return req;
    }

    public static Optional<WebPath> from(FullHttpRequest req) {

        return Optional.of(req.uri())
            .filter(uri ->
                !uri.isBlank())
            .flatMap(uri -> {
                String decoded = URLDecoder.decode(uri, StandardCharsets.UTF_8);
                return Prefix.getFor(decoded).map(prefix ->
                    new WebPath(prefix, decoded, req));
            });
    }

    public boolean isKeepAlive() {

        return HttpUtil.isKeepAlive(req);
    }

    public boolean isFlac() {

        return req.uri().endsWith(".flac");
    }

    private static Map<QPar, String> params(String uri, int queryIndex) {

        return queryIndex < 0
            ? Collections.emptyMap()
            : URLs.queryParams(uri.substring(queryIndex + 1));
    }

    private Optional<UUID> authentication(HttpRequest req) {

        return Optional.of(req.headers())
            .map(headers ->
                headers.get(COOKIE))
            .map(ServerCookieDecoder.STRICT::decode)
            .stream()
            .flatMap(Collection::stream)
            .filter(cookie ->
                cookie.name().equalsIgnoreCase(GUI.ID_COOKIE))
            .map(cookie ->
                UUID.fromString(cookie.value()))
            .findFirst()
            .or(() -> get(QPar.STREAMLEASE));
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + prefix.getPref() + path + "]";
    }
}
