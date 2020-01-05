package mediaserver.http;

import mediaserver.util.Sourced;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static mediaserver.util.Sourced.Type.SOURCES;

public final class WebCache<K, V> {

    private final Map<K, Sourced<V>> cache = new ConcurrentHashMap<>();

    private final Function<K, Sourced<V>> loader;

    public WebCache(Function<K, Sourced<V>> loader) {

        this.loader = loader;
    }

    public Optional<V> get(K key) {

        Sourced<V> cached = cache.computeIfAbsent(key, loader);
        return cached
            .unpackTyped(
                SOURCES,
                obj ->
                    loader.apply(key).unpack())
            .orElseGet(cached::unpack);
    }
}
