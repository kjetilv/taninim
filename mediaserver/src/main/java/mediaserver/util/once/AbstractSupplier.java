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

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

abstract class AbstractSupplier<T> implements Supplier<T> {

    private final Supplier<T> supplier;

    AbstractSupplier(Supplier<T> supplier) {
        this.supplier = Objects.requireNonNull(supplier, "supplier");
    }

    @Override
    public final T get() {
        return get(supplier, true);
    }

    final Supplier<Optional<T>> maybe() {
        return () ->
            Optional.ofNullable(get(supplier, false));
    }

    protected abstract T get(Supplier<T> supplier, boolean required);

    @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + supplier + "]";
    }
}
