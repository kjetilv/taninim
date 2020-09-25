package mediaserver.util;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

public class ExpiringState<T> implements Function<Instant, Optional<T>>, Supplier<Optional<T>> {

    private final AtomicReference<T> state = new AtomicReference<>();

    private final AtomicReference<Instant> lastSet = new AtomicReference<>();

    private final Duration duration;

    private final Supplier<Instant> clock;

    public ExpiringState(Duration duration) {
        this(duration, null);
    }

    private ExpiringState(Duration duration, Supplier<Instant> clock) {
        this.duration = Objects.requireNonNull(duration, "duration");
        this.clock = clock == null ? Clock.systemDefaultZone()::instant : clock;
    }

    @Override
    public Optional<T> apply(Instant instant) {
        return get(instant);
    }

    @Override
    public Optional<T> get() {
        return get(clock.get());
    }

    public boolean set(Instant time, T value) {
        return set(time, value, false);
    }

    public boolean set(Instant time, T value, boolean force) {
        synchronized (state) {
            if (force || expiredAt(time)) {
                state.set(value);
                if (value != null) {
                    lastSet.set(time);
                }
                return true;
            }
            return false;
        }
    }

    public Optional<T> get(Instant time) {
        return get(time, null);
    }

    public Optional<T> get(Instant time, Supplier<Optional<T>> newValue) {
        synchronized (state) {
            if (lastSet.get() == null || expiredAt(time)) {
                T value = Optional.ofNullable(newValue).flatMap(Supplier::get).orElse(null);
                set(time, value, true);
            }
            return Optional.ofNullable(state.get());
        }
    }

    public Optional<Duration> getRemaining(Instant time) {
        synchronized (state) {
            return expiredAt(time) ? Optional.empty() : remaining(time);
        }
    }

    public boolean expire() {
        synchronized (state) {
            T oldValue = state.getAndSet(null);
            lastSet.set(null);
            return oldValue != null;
        }
    }

    private boolean expiredAt(Instant time) {
        synchronized (state) {
            Instant lastSetTime = lastSet.get();
            return lastSetTime == null || now(time).isAfter(lastSetTime.plus(duration));
        }
    }

    private Optional<Duration> remaining(Temporal time) {
        return Optional.of(lastSet.get())
            .map(ins -> ins.plus(duration))
            .map(exp -> Duration.between(time, exp));
    }

    private Instant now(Instant time) {
        return time == null ? clock.get() : time;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + state + " @ " + lastSet + "]";
    }
}
