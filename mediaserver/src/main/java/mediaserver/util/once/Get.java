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
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class Get {

    /**
     * Returns a supplier which runs the given supplier once.
     *
     * @param supplier Source supplier
     * @param <T> Type
     *
     * @return Single-run supplier
     */
    public static <T> Supplier<T> once(Supplier<T> supplier) {
        return vet(supplier) instanceof Once<?> ? supplier : new Once<>(supplier);
    }

    public static <T> Supplier<T> mostlyOnce(Supplier<T> supplier) {
        return vet(supplier) instanceof MostlyOnce<?> ? supplier : new MostlyOnce<>(supplier);
    }

    public static <T> Supplier<Optional<T>> maybeOnce(Supplier<T> supplier) {
        Supplier<T> tSupplier = vet(supplier) instanceof Once<?>
            ? supplier
            : mostlyOnce(supplier);
        return ((AbstractSupplier<T>) tSupplier).maybe();
    }

    public static <T> void ifPresent(Supplier<T> supplier, Consumer<T> then) {
        ifExists(supplier).ifPresent(then);
    }

    public static <T> Optional<T> ifExists(Supplier<T> supplier) {
        return maybeOnce(supplier).get();
    }

    private Get() {
    }

    private static <T> T vet(T supplier) {
        return Objects.requireNonNull(supplier, "supplier");
    }
}
