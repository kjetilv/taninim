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

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public abstract class AbstractHashable
    implements Hashable, Serializable {

    private final AtomicReference<UUID> hash = new AtomicReference<>();

    private final AtomicReference<String> toString = new AtomicReference<>();

    private static final String HASH = "MD5";

    private static final long serialVersionUID = -2993413752909173835L;

    @Override
    public final UUID getUuid() {
        return hash.updateAndGet(v ->
            v == null
                ? uuid()
                : v);
    }

    protected static void hash(Consumer<byte[]> hash, byte[]... justBytes) {
        for (byte[] justByte : justBytes) {
            hash.accept(justByte);
        }
    }

    protected static void hash(Consumer<byte[]> hash, String... strings) {
        hashStrings(hash, Arrays.asList(strings));
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

    protected static void hash(Consumer<byte[]> h, Collection<? extends Hashable> hasheds) {
        for (Hashable hashable : hasheds) {
            if (hashable != null) {
                hashable.hashTo(h);
            }
        }
    }

    protected Object toStringBody() {
        return null;
    }

    private Object toStringIdentifier() {
        String hash = getUuid().toString();
        return hash.substring(0, hash.indexOf("-"));
    }

    private UUID uuid() {
        MessageDigest md5 = md5();
        hashTo(md5::update);
        return UUID.nameUUIDFromBytes(md5.digest());
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
        return obj == this || obj != null && obj.getClass() == getClass()
            && ((Hashed) obj).getUuid().equals(getUuid());
    }

    @Override
    public final String toString() {
        return toString.updateAndGet(v ->
            v == null
                ? getClass().getSimpleName() + '[' + toStringIdentifier() + toStringContents() + ']'
                : v);
    }
}
