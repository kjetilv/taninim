package mediaserver.gui;

import mediaserver.http.*;

public final class Resources extends AbstractResources {

    private final String resourcePrefix;

    public Resources(Route route, WebCache<String, byte[]> webCache, String resourcePrefix) {

        super(route, webCache);
        this.resourcePrefix = resourcePrefix;
    }

    @Override
    protected Handling handle(Req req) {

        return handle(req, resourcePrefix + req.getPath());
    }
}
