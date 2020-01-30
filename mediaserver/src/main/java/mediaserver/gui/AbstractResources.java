package mediaserver.gui;

import mediaserver.http.*;
import mediaserver.util.Sourced;

public abstract class AbstractResources extends NettyHandler {

    protected final WebCache<String, byte[]> webCache;

    public AbstractResources(
        WebCache<String, byte[]> webCache,
        Page... pages
    ) {

        super(pages);
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
