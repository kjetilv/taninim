package mediaserver.gui;

import mediaserver.http.*;

public final class Resources extends NettyHandler {

    private final String resourcePrefix;

    private final WebCache<String, byte[]> cache;

    public Resources(String resourcePrefix, WebCache<String, byte[]> cache) {

        super(Page.RES);
        this.resourcePrefix = resourcePrefix;
        this.cache = cache;
    }

    @Override
    public Handling handleRequest(WebPath webPath) {

        return cache.get(resourcePrefix + webPath.getPath())
            .map(bytes ->
                handle(webPath, bytes))
            .orElseGet(() ->
                handleNotFound(webPath));
    }
}
