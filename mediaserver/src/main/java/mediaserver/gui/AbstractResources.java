package mediaserver.gui;

import java.time.Duration;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import mediaserver.http.Handling;
import mediaserver.http.Headers;
import mediaserver.http.NettyHandler;
import mediaserver.http.Req;
import mediaserver.http.Route;
import mediaserver.http.WebCache;
import mediaserver.util.IO;

abstract class AbstractResources extends NettyHandler {

    private final WebCache<String, byte[]> webCache;

    AbstractResources(
        @Nonnull Route route,
        @Nonnull WebCache<String, byte[]> webCache
    ) {
        super(route);
        this.webCache = Objects.requireNonNull(webCache, "webCache");
    }

    protected @Nonnull Handling handle(Req req, String key) {
        return webCache.get(key)
            .map((type, bytes) ->
                handle(
                    req,
                    bytes,
                    cacheable(type, Duration.ofDays(1))))
            .unpack()
            .orElseGet(() ->
                handleNotFound(req));
    }

    @Nullable
    private Headers cacheable(IO.Type type, Duration time) {
        return type == IO.Type.JAR ? cacheable(time) : null;
    }
}
