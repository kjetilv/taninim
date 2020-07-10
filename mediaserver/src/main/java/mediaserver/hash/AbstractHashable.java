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

import mediaserver.util.Print;

public abstract class AbstractHashable
    implements Hashable, Serializable {

    private final AtomicReference<UUID> hash = new AtomicReference<>();

    private final AtomicReference<String> toString = new AtomicReference<>();

    @Override
    public final UUID getUuid() {
        return hash.updateAndGet(v -> v == null ? uuid() : v);
    }

    protected static void hash(Consumer<byte[]> hash, byte[]... bytes) {
        for (byte[] bite: bytes) {
            hash.accept(bite);
        }
    }

    protected static void hash(Consumer<byte[]> hash, String... strings) {
        hashStrings(hash, Arrays.asList(strings));
    }

    protected static void hash(Consumer<byte[]> hash, Integer... values) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES * values.length);
        for (Integer value: values) {
            if (value != null) {
                buffer.putInt(value);
            }
        }
        hash.accept(buffer.array());
    }

    protected static void hash(Consumer<byte[]> h, Hashable... hasheds) {
        hash(h, Arrays.asList(hasheds));
    }

    protected static void hash(Consumer<byte[]> h, Collection<? extends Hashable> hasheds) {
        for (Hashable hashable: hasheds) {
            if (hashable != null) {
                hashable.hashTo(h);
            }
        }
    }

    protected abstract StringBuilder withStringBody(StringBuilder sb);

    private String build() {
        return withStringContents(
            withStringIdentifier(
                new StringBuilder(getClass().getSimpleName())
                    .append('['))
                .append("<")
        ).append(">]").toString();
    }

    private StringBuilder withStringIdentifier(StringBuilder sb) {
        return sb.append(Print.uuid(getUuid()));
    }

    private UUID uuid() {
        MessageDigest md5 = md5();
        hashTo(md5::update);
        return UUID.nameUUIDFromBytes(md5.digest());
    }

    private StringBuilder withStringContents(StringBuilder sb) {
        int length = sb.length();
        StringBuilder sb2 = sb.append(' ');
        StringBuilder sb3 = withStringBody(sb2);
        if (sb3.length() == sb2.length()) {
            return sb2.delete(length, length + 1);
        }
        return sb2;
    }

    private static final String HASH = "MD5";

    private static final long serialVersionUID = -2993413752909173835L;

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

    @Override
    public final boolean equals(Object obj) {
        return obj == this || obj != null && obj.getClass() == getClass()
            && ((Hashed) obj).getUuid().equals(getUuid());
    }

    @Override
    public final int hashCode() {
        return getUuid().hashCode();
    }

    @Override
    public final String toString() {
        return toString.updateAndGet(v -> v == null ? build() : v);
    }
}
