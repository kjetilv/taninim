package mediaserver.http;

import mediaserver.hash.Hashable;
import mediaserver.util.Pair;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class QueryParametersTracker {

    private final Map<QPar, Collection<? extends Hashable>> values = new EnumMap<>(QPar.class);

    public QueryParametersTracker set(QPar par, Collection<? extends Hashable> values) {

        this.values.put(par, values);
        return this;
    }

    public String add(QPar par, Hashable value) {

        return collate(e ->
            link(e.getKey(), e.getValue(), par, value, null));
    }

    public String remove(QPar par, Hashable value) {

        return collate(e ->
            link(e.getKey(), e.getValue(), par, null, value));
    }

    @SuppressWarnings("MethodMayBeStatic")
    public String focus(QPar par, Hashable value) {

        return link(par, value);
    }

    private String collate(
        Function<Map.Entry<QPar, Collection<? extends Hashable>>, Stream<Pair<QPar, Hashable>>> mapper
    ) {

        return values.entrySet().stream()
            .flatMap(mapper)
            .map(pair -> link(pair.getT1(), pair.getT2()))
            .collect(Collectors.joining("&"));
    }

    private static Stream<Pair<QPar, Hashable>> link(
        QPar par,
        Collection<? extends Hashable> values,
        QPar changeType,
        Hashable add,
        Hashable remove
    ) {

        return getStream(par, values, changeType, add, remove)
            .map(hashable -> Pair.of(par, hashable));
    }

    private static Stream<? extends Hashable> getStream(
        QPar par,
        Collection<? extends Hashable> values,
        QPar changeType,
        Hashable add,
        Hashable remove
    ) {

        if (par == changeType) {
            if (add != null) {
                return Stream.concat(values.stream(), Stream.of(add)).distinct();
            }
            if (remove != null) {
                return values.stream().filter(v -> !v.equals(remove));
            }
        }
        return values.stream();
    }

    private static String link(QPar par, Hashable value) {

        return String.format("%s=%s", par.getName(), value.getUuid());
    }
}
