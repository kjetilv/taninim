package mediaserver.gui;

import mediaserver.http.Handling;
import mediaserver.http.Page;
import mediaserver.http.WebCache;
import mediaserver.http.Req;

public final class Resources extends AbstractResources {

    private final String resourcePrefix;

    public Resources(WebCache<String, byte[]> webCache, String resourcePrefix) {

        super(webCache, Page.RES);
        this.resourcePrefix = resourcePrefix;
    }

    @Override
    protected Handling handleRequest(Req req) {

        return handle(req, resourcePrefix + req.getPath());
    }
}
