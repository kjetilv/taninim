package mediaserver.util;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class UpdateDetector implements BooleanSupplier {

    private final AtomicReference<Instant> lastUpdate = new AtomicReference<>();

    private final Supplier<Instant> updater;

    public UpdateDetector(Supplier<Instant> updater) {

        this.updater = updater;
    }

    @Override
    public boolean getAsBoolean() {

        AtomicBoolean updated = new AtomicBoolean();
        lastUpdate.updateAndGet(instant -> {
            Instant newUpdate = updater.get();
            if (instant == null || newUpdate.isAfter(instant)) {
                updated.set(true);
                return newUpdate;
            }
            return instant;
        });
        return updated.get();
    }
}
