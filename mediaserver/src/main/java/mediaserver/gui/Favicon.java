package mediaserver.gui;

import java.util.Objects;

import mediaserver.http.Handling;
import mediaserver.http.Req;
import mediaserver.http.Route;
import mediaserver.http.WebCache;

public final class Favicon extends AbstractResources {

    private final String resource;

    public Favicon(Route route, WebCache<String, byte[]> webCache, String resource) {
        super(route, webCache);
        this.resource = Objects.requireNonNull(resource, "resource");
    }

    protected @Override
    Handling handle(Req req) {
        return handle(req, resource);
    }
}
