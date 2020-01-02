package mediaserver.gui;

import mediaserver.http.*;

public final class Favicon extends NettyHandler {

    private final WebCache<String, byte[]> webCache;

    private final String resource;

    public Favicon(WebCache<String, byte[]> webCache, String resource) {

        super(Page.FAVICON_ICO);
        this.webCache = webCache;
        this.resource = resource;
    }

    @Override
    public Handling handleRequest(WebPath webPath) {

        return webCache.get(resource)
            .map(bytes ->
                handle(webPath, bytes))
            .orElseGet(() ->
                handleNotFound(webPath));
    }
}
