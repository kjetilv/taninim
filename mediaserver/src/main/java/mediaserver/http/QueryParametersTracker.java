package mediaserver.http;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import mediaserver.hash.Hashable;

public final class QueryParametersTracker {

    private final Map<QPar, Collection<? extends Hashable>> parameterGroups = new EnumMap<>(QPar.class);

    public QueryParametersTracker set(QPar par, Collection<? extends Hashable> values) {
        this.parameterGroups.put(par, values);
        return this;
    }

    public String set(QPar par, Object value) {
        String link = collate(parameterGroup ->
            pairs(parameterGroup.getKey(), parameterGroup.getValue(), null, null, null));
        return link.isBlank() ? asLink(par, value) : link + "&" + asLink(par, value);
    }

    public String add(QPar par, Hashable value) {
        return collate(entry ->
            pairs(entry.getKey(), entry.getValue(), par, value, null));
    }

    public String remove(QPar par, Hashable value) {
        return collate(entry ->
            pairs(entry.getKey(), entry.getValue(), par, null, value));
    }

    @SuppressWarnings("MethodMayBeStatic")
    public String focus(QPar par, Hashable value) {
        return asLink(par, value.getUuid());
    }

    public boolean isMulti() {
        return parameterGroups.values().stream().mapToInt(Collection::size).sum() > 1L;
    }

    private String collate(
        Function<Map.Entry<QPar, Collection<? extends Hashable>>, Stream<Map.Entry<QPar, Hashable>>> mapper
    ) {
        return parameterGroups.entrySet().stream()
            .flatMap(mapper)
            .map(entry ->
                asLink(entry.getKey(), entry.getValue().getUuid()))
            .collect(Collectors.joining("&"));
    }

    private static Stream<Map.Entry<QPar, Hashable>> pairs(
        QPar par,
        Collection<? extends Hashable> values,
        QPar changeType,
        Hashable add,
        Hashable remove
    ) {
        Stream<? extends Hashable> items = par == changeType
            ? addedOrRemoved(values, add, remove)
            : values.stream();
        return items.map(hashable -> new AbstractMap.SimpleEntry<>(par, hashable));
    }

    private static Stream<? extends Hashable> addedOrRemoved(
        Collection<? extends Hashable> values,
        Hashable add,
        Hashable remove
    ) {
        if (add != null) {
            return withAdded(values, add);
        }
        if (remove != null) {
            return withRemoved(values, remove);
        }
        return values.stream();
    }

    private static Stream<? extends Hashable> withRemoved(Collection<? extends Hashable> values, Hashable remove) {
        return values.stream().filter(v -> !v.equals(remove));
    }

    private static Stream<? extends Hashable> withAdded(Collection<? extends Hashable> values, Hashable add) {
        return Stream.concat(values.stream(), Stream.of(add)).distinct();
    }

    private static String asLink(QPar par, Object value) {
        return String.format("%s=%s", par.name(), value);
    }
}
