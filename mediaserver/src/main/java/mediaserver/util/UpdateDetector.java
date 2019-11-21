package mediaserver.util;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class UpdateDetector implements BooleanSupplier {

    private final AtomicReference<Instant> lastUpdate = new AtomicReference<>();

    private final Supplier<Optional<Instant>> updater;

    public UpdateDetector(Supplier<Optional<Instant>> updater) {

        this.updater = Objects.requireNonNull(updater);
        this.updater.get().ifPresent(lastUpdate::set);
    }

    @Override
    public boolean getAsBoolean() {

        AtomicBoolean updated = new AtomicBoolean();
        lastUpdate.updateAndGet(instant ->
            updater.get().map(newUpdate -> {
                if (newUpdate.isAfter(instant)) {
                    updated.set(true);
                    return newUpdate;
                }
                return instant;
            }).orElse(instant));
        return updated.get();
    }
}
