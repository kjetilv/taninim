package mediaserver.http;

import mediaserver.util.Sourced;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public final class WebCache<K, V> {

    private final Map<K, Sourced.Type> cachedSourceTypes = new ConcurrentHashMap<>();

    private final Map<K, Sourced<V>> cache = new ConcurrentHashMap<>();

    private final Function<K, Sourced<V>> loader;

    public WebCache(Function<K, Sourced<V>> loader) {

        this.loader = loader;
    }

    public Sourced<V> get(K key) {

        AtomicBoolean wasLoaded = new AtomicBoolean();
        Sourced.Type type = cachedSourceTypes.computeIfAbsent(key, __ ->
            cache.computeIfAbsent(key, k -> {
                try {
                    return loader.apply(k);
                } finally {
                    wasLoaded.set(true);
                }
            }).sourceType());
        return type == Sourced.Type.SOURCES && !wasLoaded.get()
            ? loader.apply(key)
            : cache.computeIfAbsent(key, loader);
    }
}
