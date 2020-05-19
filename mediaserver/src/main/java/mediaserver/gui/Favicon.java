package mediaserver.gui;

import mediaserver.http.*;

public final class Favicon extends AbstractResources {

    private final String resource;

    public Favicon(Route route, WebCache<String, byte[]> webCache, String resource) {

        super(route, webCache);
        this.resource = resource;
    }

    @Override
    protected Handling handle(Req req) {

        return handle(req, resource);
    }
}
