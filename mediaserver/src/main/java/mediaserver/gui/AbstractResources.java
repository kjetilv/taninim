package mediaserver.gui;

import mediaserver.http.*;

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
            .map(bytes ->
                handle(req, bytes, CACHEABLE))
            .orElseGet(() ->
                handleNotFound(req));
    }
}
