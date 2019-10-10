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

package mediaserver.hash;

import mediaserver.util.MostlyOnce;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public abstract class AbstractHashable implements Hashable {

    /**
     * A supplier which computes {@link Hashable this hashable's} uuid with a {@link mediaserver.util.MostlyOnce}.
     */
    private final Supplier<UUID> hash = MostlyOnce.get(uuid(this));

    private final Supplier<String> toString =
        MostlyOnce.get(() ->
            getClass().getSimpleName() + '[' + toStringIdentifier() + toStringContents() + ']');

    private static final String HASH = "MD5";

    private static final byte[] NO_TRUTH = "none".getBytes();

    private static final byte[] TRUTHYNESS = "true".getBytes();

    private static final byte[] FALSYNESS = "false".getBytes();

    @Override
    public final UUID getUuid() {
        return hash.get();
    }

    protected static void hash(Consumer<byte[]> hash, String... strings) {
        hashStrings(hash, Arrays.asList(strings));
    }

    protected static void hash(Consumer<byte[]> h, Hashed... ids) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES * 2 * ids.length);
        for (Hashed id : ids) {
            UUID uuid = id.getUuid();
            buffer.putLong(uuid.getMostSignificantBits());
            buffer.putLong(uuid.getLeastSignificantBits());
        }
        h.accept(buffer.array());
    }

    protected static void hash(Consumer<byte[]> h, Hashable... hashables) {
        hash(h, Arrays.asList(hashables));
    }

    protected static void hash(Consumer<byte[]> hash, Long... values) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES * values.length);
        for (Long value : values) {
            if (value != null) {
                buffer.putLong(value);
            }
        }
        hash.accept(buffer.array());
    }

    protected static void hashLongs(Consumer<byte[]> hash, long... values) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES * values.length);
        for (long value : values) {
            buffer.putLong(value);
        }
        hash.accept(buffer.array());
    }

    protected static void hash(Consumer<byte[]> hash, Integer... values) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES * values.length);
        for (Integer value : values) {
            if (value != null) {
                buffer.putInt(value);
            }
        }
        hash.accept(buffer.array());
    }

    protected static void hashInts(Consumer<byte[]> hash, int... values) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES * values.length);
        for (int value : values) {
            buffer.putInt(value);
        }
        hash.accept(buffer.array());
    }

    protected static void hash(Consumer<byte[]> h, Collection<? extends Hashable> hasheds) {
        for (Hashable hashable : hasheds) {
            if (hashable != null) {
                hashable.hashTo(h);
            }
        }
    }

    protected static void hashBools(Consumer<byte[]> h, Boolean... truths) {
        for (Boolean truth : truths) {
            h.accept(truth == null ? NO_TRUTH : truth ? TRUTHYNESS : FALSYNESS);
        }
    }

    protected Object toStringBody() {
        return null;
    }

    private Object toStringIdentifier() {
        String hash = getUuid().toString();
        return hash.substring(0, hash.indexOf("-"));
    }

    /**
     * Takes a {@link Hashable hashable} and returns a supplier which computs its UUID
     *
     * @param hashable Hashable
     * @return UUID supplier
     */
    private static Supplier<UUID> uuid(Hashable hashable) {
        return MostlyOnce.get(() -> {
            MessageDigest md5 = md5();
            hashable.hashTo(md5::update);
            return UUID.nameUUIDFromBytes(md5.digest());
        });
    }

    private static MessageDigest md5() {
        try {
            return MessageDigest.getInstance(HASH);
        } catch (Exception e) {
            throw new IllegalStateException("Expected " + HASH + " implementation", e);
        }
    }

    private static void hashStrings(Consumer<byte[]> hash, Collection<String> strings) {
        strings.stream()
            .filter(Objects::nonNull)
            .forEach(s ->
                hash.accept(s.getBytes(StandardCharsets.UTF_8)));
    }

    private String toStringContents() {
        Object body = toStringBody();
        if (body == null) {
            return "";
        }
        String string = body.toString().trim();
        if (string.isBlank()) {
            return "";
        }
        return ' ' + string;
    }

    @Override
    public final int hashCode() {
        return getUuid().hashCode();
    }

    @Override
    public final boolean equals(Object obj) {
        return obj == this || obj.getClass() == getClass()
            && ((Hashed) obj).getUuid().equals(getUuid());
    }

    @Override
    public final String toString() {
        return toString.get();
    }
}
