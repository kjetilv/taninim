package mediaserver.gui;

import mediaserver.http.*;
import mediaserver.util.Sourced;

public abstract class AbstractResources extends NettyHandler {

    private final WebCache<String, byte[]> webCache;

    AbstractResources(WebCache<String, byte[]> webCache, Route page) {

        super(page);
        this.webCache = webCache;
    }

    protected Handling handle(Req req, String key) {

        return webCache.get(key)
            .map((type, bytes) ->
                handle(req, bytes, type == Sourced.Type.JAR ? CACHEABLE : null))
            .unpack()
            .orElseGet(() ->
                handleNotFound(req));
    }
}
