package mediaserver.util.once;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import mediaserver.util.Pair;

public final class Apply {

    public static <F, T> Function<F, T> memoized(Function<F, T> fun) {
        return memoized(fun, null, DEFAULT_SIZE);
    }

    public static <F, T> Function<F, T> memoized(Function<F, T> fun, int size) {
        return memoized(fun, null, size);
    }

    public static <F, T> Function<F, T> memoized(Function<F, T> fun, Supplier<Long> time) {
        return memoized(fun, time, DEFAULT_SIZE);
    }

    public static <F, T> Function<F, T> memoized(Function<F, T> fun, Supplier<Long> time, int size) {
        Map<F, Pair<Long, T>> memo = new ConcurrentHashMap<>();
        return f -> memo.computeIfAbsent(f, __ -> {
            try {
                return Pair.of(
                    time == null
                        ? System.currentTimeMillis()
                        : time.get(),
                    fun.apply(f));
            } finally {
                clearOld(size, memo);
            }
        }).getT2();
    }

    private Apply() {
    }

    private static final int DEFAULT_SIZE = 128;

    private static <F, T> void clearOld(int size, Map<F, Pair<Long, T>> memo) {
        if (memo.size() > size) {
            new HashSet<>(memo.entrySet()).stream()
                .sorted(Comparator.comparing(e -> e.getValue().getT1()))
                .forEach(e -> memo.remove(e.getKey()));
        }
    }
}
