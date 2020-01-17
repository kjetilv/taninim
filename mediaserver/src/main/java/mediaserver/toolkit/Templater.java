package mediaserver.toolkit;

import mediaserver.gui.Template;
import mediaserver.http.WebCache;

public final class Templater {

    private final WebCache<String, String> cache;

    protected static final String ALBUM = "album";

    public Templater(WebCache<String, String> cache) {

        this.cache = cache;
    }

    public Template template(String resource) {

        return cache.get(resource)
            .map(source ->
                new Template(resource, source))
            .orElseThrow(() ->
                new IllegalArgumentException("No such template resource: " + resource));
    }
}
