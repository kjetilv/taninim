package mediaserver.gui;

import mediaserver.http.WebCache;
import mediaserver.util.IO;

public final class Templater {

    private final WebCache<String, String> cache;

    protected static final String ALBUM = "album";

    public Templater() {

        this.cache = new WebCache<>(IO::read);
    }

    protected Template template(String resource) {

        return cache.get(resource)
            .map(source ->
                new Template(resource, source))
            .orElseThrow(() ->
                new IllegalArgumentException("No such template resource: " + resource));
    }
}
