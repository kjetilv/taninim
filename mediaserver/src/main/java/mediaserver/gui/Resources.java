package mediaserver.gui;

import mediaserver.http.Handling;
import mediaserver.http.Req;
import mediaserver.http.Route;
import mediaserver.http.WebCache;

public final class Resources extends AbstractResources {

    private final String resourcePrefix;

    public Resources(WebCache<String, byte[]> webCache, String resourcePrefix) {

        super(webCache, Route.RES);
        this.resourcePrefix = resourcePrefix;
    }

    @Override
    protected Handling handle(Req req) {

        return handle(req, resourcePrefix + req.getPath());
    }
}
