package mediaserver.gui;

import mediaserver.http.*;

public final class Resources extends AbstractResources {

    private final String resourcePrefix;

    public Resources(WebCache<String, byte[]> webCache, String resourcePrefix) {

        super(webCache, Page.RES);
        this.resourcePrefix = resourcePrefix;
    }

    @Override
    public Handling handleRequest(WebPath webPath) {

        return handle(webPath, resourcePrefix + webPath.getPath());
    }
}
