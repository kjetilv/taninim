package mediaserver.http;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import mediaserver.util.IO;
import mediaserver.util.Sourced;

public final class WebCache<K, V> {

    private final Map<K, IO.Type> cachedSourceTypes = new ConcurrentHashMap<>();

    private final Map<K, Sourced<V>> cache = new ConcurrentHashMap<>();

    private final Function<K, Sourced<V>> loader;

    public WebCache(Function<K, Sourced<V>> loader) {
        this.loader = loader;
    }

    public Sourced<V> get(K key) {
        AtomicBoolean wasLoaded = new AtomicBoolean();
        IO.Type type = cachedSourceTypes.computeIfAbsent(key, __ ->
            cache.computeIfAbsent(key, k -> {
                try {
                    return loader.apply(k);
                } finally {
                    wasLoaded.set(true);
                }
            }).sourceType());
        return type == IO.Type.SOURCES && !wasLoaded.get()
            ? loader.apply(key)
            : cache.computeIfAbsent(key, loader);
    }
}
