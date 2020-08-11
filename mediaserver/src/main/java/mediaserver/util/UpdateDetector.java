package mediaserver.util;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class UpdateDetector implements BooleanSupplier {
    
    private final AtomicReference<Instant> lastUpdate = new AtomicReference<>();
    
    
    private final Supplier<Optional<Instant>> newUpdate;
    
    public UpdateDetector( Supplier<Optional<Instant>> newUpdate) {
        this.newUpdate = Objects.requireNonNull(newUpdate);
        this.newUpdate.get().ifPresent(lastUpdate::set);
    }
    
    @Override
    public boolean getAsBoolean() {
        AtomicBoolean wasUpdated = new AtomicBoolean();
        lastUpdate.updateAndGet(lastUpdate ->
            newUpdate.get().map(newUpdate -> {
                if (newUpdate.isAfter(lastUpdate)) {
                    wasUpdated.set(true);
                    return newUpdate;
                }
                return lastUpdate;
            }).orElse(lastUpdate));
        return wasUpdated.get();
    }
}
