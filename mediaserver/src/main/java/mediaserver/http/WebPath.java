package mediaserver.http;

import mediaserver.util.URLs;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public final class WebPath {

    private final Prefix prefix;

    private final String uri;

    private final String path;

    public WebPath(Prefix prefix, String uri) {

        this.prefix = Objects.requireNonNull(prefix, "prefix");
        this.uri = Objects.requireNonNull(uri, "uri").trim();
        int pathIndex = this.uri.indexOf("?");
        this.path = pathIndex > 0 ? this.uri.substring(0, pathIndex) : this.uri;
    }

    public Prefix getPrefix() {

        return prefix;
    }

    public String getPath() {

        return path;
    }

    public String getUri() {

        return uri;
    }

    public String contentType() {

        return path.endsWith(".css") ? "text/css"
            : path.endsWith(".js") ? "text/javascript"
            : path.endsWith(".ico") ? "image/x-icon"
            : "text/plain";
    }

    public QPars qpars() {

        return new QPars(params(uri, uri.indexOf("?")));
    }

    public static WebPath build(String uri) {

        String decoded = URLDecoder.decode(uri, StandardCharsets.UTF_8);
        return Prefix.getFor(decoded)
            .map(prefix ->
                new WebPath(prefix, prefix.resolve(decoded)))
            .orElseThrow(() ->
                new IllegalArgumentException("Unexpected path: " + decoded));
    }

    public boolean hasPrefix(Prefix prefix) {

        return this.prefix == prefix;
    }

    public boolean isAuthenticated() {

        return prefix.isAuthenticated();
    }

    private static Map<QPar, String> params(String uri, int queryIndex) {

        return queryIndex < 0
            ? Collections.emptyMap()
            : URLs.queryParams(uri.substring(queryIndex + 1));
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + prefix.getPref() + (path.isBlank() ? "" : "/" + path) + "]";
    }
}
