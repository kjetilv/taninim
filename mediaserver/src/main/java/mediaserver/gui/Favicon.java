package mediaserver.gui;

import java.util.Objects;
import javax.annotation.Nonnull;

import mediaserver.http.Handling;
import mediaserver.http.Req;
import mediaserver.http.Route;
import mediaserver.http.WebCache;

public final class Favicon extends AbstractResources {

    private final String resource;

    public Favicon(
        @Nonnull Route route,
        @Nonnull WebCache<String, byte[]> webCache,
        @Nonnull String resource
    ) {
        super(route, webCache);
        this.resource = Objects.requireNonNull(resource, "resource");
    }

    protected @Override @Nonnull Handling handle(Req req) {
        return handle(req, resource);
    }
}
