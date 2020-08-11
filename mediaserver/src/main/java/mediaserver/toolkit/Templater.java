package mediaserver.toolkit;

import java.util.Objects;

import mediaserver.http.WebCache;
import mediaserver.templates.Template;

public final class Templater {
    
    private final WebCache<String, String> cache;
    
    public Templater(WebCache<String, String> cache) {
        this.cache = Objects.requireNonNull(cache, "cache");
    }
    
    public Template template(String resource) {
        return cache.get(resource)
            .map(source ->
                new Template(resource, source))
            .unpack()
            .orElseThrow(() ->
                new IllegalArgumentException("No such template resource: " + resource));
    }
}
