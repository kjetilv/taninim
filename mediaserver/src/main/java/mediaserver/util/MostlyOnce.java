package mediaserver.util;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class MostlyOnce {

    private MostlyOnce() {
    }

    /**
     * Returns a supplier which runs the given supplier once only, mostly.
     *
     * @param supplier Source supplier, must be functional or at least idempotent
     * @param <O>      Type
     * @return Single-run supplier
     */
    public static <O> Supplier<O> get(Supplier<O> supplier) {
        return supplier == null || supplier instanceof Supp<?> ? supplier : new Supp<>(supplier);
    }

    private static final class Supp<T> implements Supplier<T> {

        private final Supplier<T> supplier;

        private final AtomicReference<T> value = new AtomicReference<>();

        private Supp(Supplier<T> supplier) {
            this.supplier = Objects.requireNonNull(supplier, "supplier");
        }

        @Override
        public T get() {
            return value.updateAndGet(v -> v == null ? supplier.get() : v);
        }
    }
}
