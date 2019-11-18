package mediaserver.util;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class UpdateDetector implements BooleanSupplier {

    private final AtomicReference<Instant> lastUpdate = new AtomicReference<>();

    private final Supplier<Instant> update;

    public UpdateDetector(Supplier<Instant> update) {

        this.update = update;
    }

    @Override
    public boolean getAsBoolean() {

        AtomicBoolean updated = new AtomicBoolean();
        lastUpdate.updateAndGet(instant -> {
            Instant newUpdate = update.get();
            if (instant == null || newUpdate.isAfter(instant)) {
                updated.set(true);
                return newUpdate;
            }
            return instant;
        });
        return updated.get();
    }
}
