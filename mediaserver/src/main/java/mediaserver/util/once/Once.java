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

package mediaserver.util.once;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

final class Once<T> extends AbstractSupplier<T> {

    private final AtomicBoolean started = new AtomicBoolean();

    private final CompletableFuture<Boolean> ok = new CompletableFuture<>();

    private final AtomicReference<T> value = new AtomicReference<>();

    private final AtomicReference<RuntimeException> error = new AtomicReference<>();

    Once(Supplier<T> supplier) {
        super(supplier);
    }

    @Override
    protected T get(Supplier<T> supplier, boolean required) {
        if (required) {
            if (started.compareAndSet(false, true)) {
                T val;
                try {
                    val = supplier.get();
                } catch (RuntimeException e) {
                    error.set(e);
                    ok.complete(false);
                    throw new IllegalStateException(this + ": failed", e);
                }
                value.set(val);
                ok.complete(true);
                return val;
            }
        } else {
            if (!ok.isDone()) {
                return null;
            }
        }
        if (ok.join()) {
            return value.get();
        }
        throw new IllegalStateException(this + ": failed", error.get());
    }
}
