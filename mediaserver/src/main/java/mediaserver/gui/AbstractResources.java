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

    protected Handling handle(WebPath webPath, String key) {

        return webCache.get(key)
            .map(bytes ->
                handle(webPath, bytes, CACHEABLE))
            .orElseGet(() ->
                handleNotFound(webPath));
    }
}
