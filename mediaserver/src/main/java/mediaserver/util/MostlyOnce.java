package mediaserver.util;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

public final class MostlyOnce {

    private MostlyOnce() {

    }

    public static <T, O> Function<T, O> compute(Function<T, O> fun) {

        return new Funn<>(vetted(fun));
    }

    /**
     * Returns a supplier which runs the given supplier mostly only once.
     *
     * @param supplier Source supplier, must be functional or at least idempotent
     * @param <O>      Type
     *
     * @return Single-run supplier
     */
    public static <O> Supplier<O> get(Supplier<O> supplier) {

        return new Supp<>(vetted(supplier));
    }

    private static <O> Supplier<O> vetted(Supplier<O> supplier) {

        if (Objects.requireNonNull(supplier, "supplier") instanceof Supp<?>) {
            throw new IllegalArgumentException("Mostly twice! " + supplier);
        }
        return supplier;
    }

    private static <T, O> Function<T, O> vetted(Function<T, O> supplier) {

        if (Objects.requireNonNull(supplier, "supplier") instanceof Funn<?, ?>) {
            throw new IllegalArgumentException("Mostly twice! " + supplier);
        }
        return supplier;
    }

    private static final class Supp<O> implements Supplier<O> {

        private final Supplier<O> supplier;

        private final AtomicReference<O> value = new AtomicReference<>();

        private Supp(Supplier<O> supplier) {

            this.supplier = Objects.requireNonNull(supplier, "supplier");
        }

        @Override
        public O get() {

            return value.updateAndGet(v -> v == null ? supplier.get() : v);
        }
    }

    private static final class Funn<T, O> implements Function<T, O> {

        private final Function<T, O> fun;

        private final AtomicReference<O> value = new AtomicReference<>();

        private Funn(Function<T, O> supplier) {

            this.fun = Objects.requireNonNull(supplier, "supplier");
        }

        @Override
        public O apply(T t) {

            return value.updateAndGet(v -> v == null ? fun.apply(t) : v);
        }
    }
}
