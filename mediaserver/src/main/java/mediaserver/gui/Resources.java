package mediaserver.gui;

import mediaserver.http.Handling;
import mediaserver.http.Route;
import mediaserver.http.WebCache;
import mediaserver.http.Req;

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
