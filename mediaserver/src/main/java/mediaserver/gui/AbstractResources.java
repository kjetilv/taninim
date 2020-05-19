package mediaserver.gui;

import mediaserver.http.*;
import mediaserver.util.Sourced;

abstract class AbstractResources extends NettyHandler {

    private final WebCache<String, byte[]> webCache;

    AbstractResources(Route route, WebCache<String, byte[]> webCache) {

        super(route);
        this.webCache = webCache;
    }

    Handling handle(Req req, String key) {

        return webCache.get(key)
            .map((type, bytes) ->
                handle(req, bytes, type == Sourced.Type.JAR ? CACHEABLE : null))
            .unpack()
            .orElseGet(() ->
                handleNotFound(req));
    }
}
