package mediaserver.util;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class ExpiringState<T> {

    private final AtomicReference<T> state = new AtomicReference<>();

    private final AtomicReference<Instant> lastSet = new AtomicReference<>();

    private final Duration duration;

    public ExpiringState(Duration duration) {

        this.duration = duration;
    }

    public boolean set(Instant time, T value) {

        return set(time, value, false);
    }

    public boolean set(Instant time, T value, boolean force) {

        synchronized (state) {
            if (force || lastSet.get() == null || expiredAt(time)) {
                state.set(value);
                lastSet.set(time);
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
                if (newValue == null) {
                    return Optional.empty();
                }
                newValue.get().ifPresent(t -> set(time, t, true));
            }
            return Optional.ofNullable(state.get());
        }
    }

    public void expire() {

        synchronized (state) {
            state.set(null);
            lastSet.set(null);
        }
    }

    private boolean expiredAt(Instant time) {

        synchronized (state) {
            Instant set = lastSet.get();
            return set != null && time.isAfter(set.plus(duration));
        }
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + state + " @ " + lastSet + "]";
    }
}
