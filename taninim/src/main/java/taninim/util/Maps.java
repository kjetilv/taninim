package taninim.util;

import module java.base;

public final class Maps {

    public static <K, V> Stream<V> get(Map<K, V> map, Predicate<K> keyLike) {
        return map.entrySet().stream()
            .filter(e -> keyLike.test(e.getKey()))
            .map(Map.Entry::getValue);
    }

    public static <K, V> Map<K, List<V>> groupBy(
        Collection<? extends V> items,
        Function<? super V, ? extends K> mapping
    ) {
        return Collections.unmodifiableMap(items.stream().collect(Collectors.groupingBy(
            mapping,
            LinkedHashMap::new,
            Collectors.toList()
        )));
    }

    public static <V, T, I extends Collection<V>> I noDuplicates(I l, Function<V, T> id) {
        if (l == null || l.size() <= 1) {
            return l;
        }
        List<Map.Entry<T, List<V>>> dupes = dupes(l, id);
        if (dupes.isEmpty()) {
            return l;
        }
        throw new IllegalStateException("Duplicates: " + dupes);
    }

    public static <K, V, T, C extends Collection<T>> Map<K, V> toMap(
        C c,
        Function<? super T, ? extends K> key,
        Function<? super T, ? extends V> val
    ) {
        if (c == null) {
            return Collections.emptyMap();
        }
        return c.stream().collect(Collectors.toMap(
            key,
            val,
            noCombine(),
            LinkedHashMap::new
        ));
    }

    private Maps() {
    }

    private static <V> BinaryOperator<V> noCombine() {
        return (v1, v2) -> {
            throw new IllegalStateException("No combine: " + v1 + " / " + v2);
        };
    }

    private static <V, T, I extends Collection<V>> List<Map.Entry<T, List<V>>> dupes(
        I l,
        Function<? super V, ? extends T> id
    ) {
        Map<T, List<V>> map = groupBy(l, id);
        return map.entrySet().stream()
            .filter(entry -> entry.getValue().size() > 1)
            .toList();
    }

}
