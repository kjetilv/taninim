package mediaserver.gui;

import mediaserver.http.Handling;
import mediaserver.http.NettyHandler;
import mediaserver.http.Req;
import mediaserver.http.Route;
import mediaserver.http.WebCache;
import mediaserver.util.IO;

abstract class AbstractResources
    extends NettyHandler {

    private final WebCache<String, byte[]> webCache;

    AbstractResources(Route route, WebCache<String, byte[]> webCache) {
        super(route);
        this.webCache = webCache;
    }

    Handling handle(Req req, String key) {
        return webCache.get(key)
            .map((type, bytes) ->
                handle(req, bytes, type == IO.Type.JAR ? CACHEABLE : null))
            .unpack()
            .orElseGet(() ->
                handleNotFound(req));
    }
}
