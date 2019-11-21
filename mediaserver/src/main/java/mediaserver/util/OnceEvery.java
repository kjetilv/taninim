/*
 *     This file is part of Unearth.
 *
 *     Unearth is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Unearth is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Unearth.  If not, see <https://www.gnu.org/licenses/>.
 */

/*
 *     This file is part of Unearth.
 *
 *     Unearth is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Unearth is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Unearth.  If not, see <https://www.gnu.org/licenses/>.
 */

package mediaserver.util;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class OnceEvery {

    private final ScheduledExecutorService service;

    public OnceEvery(ScheduledExecutorService service) {

        this.service = Objects.requireNonNull(service);
    }

    public Conditioned interval(Duration duration) {

        Objects.requireNonNull(duration, "duration");
        return new Conditioned() {
            @Override
            public Timed when(BooleanSupplier condition) {

                return new Timed() {
                    @Override
                    public <T> Supplier<T> get(Supplier<T> supplier) {

                        return new Supp<>(
                            service, duration, condition, Objects.requireNonNull(supplier, "supplier"));
                    }
                };
            }

            @Override
            public <T> Supplier<T> get(Supplier<T> supplier) {

                return new Supp<>(
                    service, duration, () -> true, Objects.requireNonNull(supplier, "supplier"));
            }
        };
    }

    public interface Conditioned {

        Timed when(BooleanSupplier condition);

        <T> Supplier<T> get(Supplier<T> supplier);
    }

    public interface Timed {

        <T> Supplier<T> get(Supplier<T> supplier);
    }

    private static final class Supp<T> implements Supplier<T> {

        private final AtomicReference<T> value = new AtomicReference<>();

        private Supp(
            ScheduledExecutorService service,
            Duration interval,
            BooleanSupplier condition,
            Supplier<T> supplier
        ) {

            this.value.set(supplier.get());
            service.scheduleAtFixedRate(
                () -> {
                    if (condition.getAsBoolean()) {
                        this.value.set(supplier.get());
                    }
                },
                interval.getSeconds(), interval.getSeconds(), TimeUnit.SECONDS);
        }

        @Override
        public T get() {

            return value.get();
        }
    }
}
