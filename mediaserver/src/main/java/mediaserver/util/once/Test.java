package mediaserver.util.once;

import java.util.function.Predicate;
import java.util.function.Supplier;

public final class Test {

    public static <T> Predicate<T> memoized(Predicate<T> fun) {
        return memoized(fun, null, DEFAULT_SIZE);
    }

    public static <T> Predicate<T> memoized(Predicate<T> fun, int size) {
        return memoized(fun, null, size);
    }

    public static <T> Predicate<T> memoized(Predicate<T> fun, Supplier<Long> time) {
        return memoized(fun, time, DEFAULT_SIZE);
    }

    public static <T> Predicate<T> memoized(Predicate<T> p, Supplier<Long> time, int size) {
        return Apply.memoized(p::test, time, size)::apply;
    }

    private Test() {
    }

    private static final int DEFAULT_SIZE = 128;
}
