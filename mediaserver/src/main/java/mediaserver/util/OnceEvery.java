package mediaserver.util;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class OnceEvery {

    public static void actuallyJustRefresh(Supplier<?>... suppliers) {
        for (Supplier<?> supplier: suppliers) {
            if (supplier instanceof Supp<?>) {
                ((Supp<?>) supplier).refresh();
            } else {
                throw new IllegalStateException("Not a refreshable: " + supplier);
            }
        }
    }

    public interface TimingBuilder {

        Timed when(BooleanSupplier condition);

        <T> Supplier<T> get(Supplier<? extends T> supplier);
    }

    public interface Timed {

        <T> Supplier<T> get(Supplier<? extends T> supplier);
    }

    private final ScheduledExecutorService service;

    public OnceEvery(ScheduledExecutorService service) {
        this.service = Objects.requireNonNull(service);
    }

    public TimingBuilder interval(Duration duration) {
        Objects.requireNonNull(duration, "duration");
        return new TimingBuilder() {

            @Override
            public Timed when(BooleanSupplier condition) {
                return new Timed() {

                    @Override
                    public <T> Supplier<T> get(Supplier<? extends T> supplier) {
                        return new Supp<>(
                            service, duration, condition, Objects.requireNonNull(supplier, "supplier"));
                    }
                };
            }

            @Override
            public <T> Supplier<T> get(Supplier<? extends T> supplier) {
                return new Supp<>(
                    service, duration, () -> true, Objects.requireNonNull(supplier, "supplier"));
            }
        };
    }

    private static final class Supp<T> implements Supplier<T> {

        private final AtomicReference<T> value = new AtomicReference<>();

        private final Supplier<? extends T> supplier;

        private Supp(
            ScheduledExecutorService service,
            Duration interval,
            BooleanSupplier condition,
            Supplier<? extends T> supplier
        ) {
            this.supplier = supplier;
            refresh();
            service.scheduleAtFixedRate(
                () -> {
                    if (condition.getAsBoolean()) {
                        refresh();
                    }
                },
                interval.getSeconds(), interval.getSeconds(), TimeUnit.SECONDS);
        }

        @Override
        public T get() {
            return value.get();
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + supplier + "]";
        }

        private void refresh() {
            this.value.set(supplier.get());
        }
    }
}
