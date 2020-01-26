package mediaserver.gui;

import mediaserver.http.Handling;
import mediaserver.http.Page;
import mediaserver.http.WebCache;
import mediaserver.http.Req;

public final class Favicon extends AbstractResources {

    private final String resource;

    public Favicon(WebCache<String, byte[]> webCache, String resource) {

        super(webCache, Page.FAVICON_ICO);
        this.resource = resource;
    }

    @Override
    protected Handling handleRequest(Req req) {

        return handle(req, resource);
    }
}
