package mediaserver.gui;

import mediaserver.util.Sourced;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static mediaserver.util.Sourced.Type.SOURCES;

public class WebCache<K, V> {

    private final Map<K, Sourced<V>> cache = new ConcurrentHashMap<>();

    private final Function<K, Sourced<V>> heavyDuty;

    public WebCache(Function<K, Sourced<V>> heavyDuty) {

        this.heavyDuty = heavyDuty;
    }

    public Optional<V> get(K key) {
        Sourced<V> sourced = computedValue(key);
        if (sourced.source() == SOURCES) {
            return heavyDuty.apply(key).unpack();
        }
        return sourced.unpack();
    }

    public Sourced<V> computedValue(K key) {

        return cache.computeIfAbsent(key, __ ->
                Objects.requireNonNull(heavyDuty.apply(key), "Null sourced"));
    }

}
