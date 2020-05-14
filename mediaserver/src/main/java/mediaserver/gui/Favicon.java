package mediaserver.gui;

import mediaserver.http.Handling;
import mediaserver.http.Req;
import mediaserver.http.Route;
import mediaserver.http.WebCache;

public final class Favicon extends AbstractResources {

    private final String resource;

    public Favicon(WebCache<String, byte[]> webCache, String resource) {

        super(webCache, Route.FAVICON_ICO);
        this.resource = resource;
    }

    @Override
    protected Handling handle(Req req) {

        return handle(req, resource);
    }
}
