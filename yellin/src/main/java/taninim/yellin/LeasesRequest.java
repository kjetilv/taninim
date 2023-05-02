package taninim.yellin;

import java.util.Map;
import java.util.function.Function;

import com.github.kjetilv.uplift.kernel.uuid.Uuid;

import static java.util.Objects.requireNonNull;

public record LeasesRequest(
    String userId,
    Uuid token,
    Op op,
    Uuid album
) {

    public static LeasesRequest acquire(
        String body,
        Function<? super String, ? extends Map<String, Object>> jsonParser
    ) {
        return fromMap(Op.ACQUIRE, body, jsonParser);
    }

    public static LeasesRequest release(
        String body,
        Function<? super String, ? extends Map<String, Object>> jsonParser
    ) {
        return fromMap(Op.RELEASE, body, jsonParser);
    }

    public enum Op {

        ACQUIRE,
        RELEASE
    }

    public LeasesRequest(String userId, Uuid token, Op op, Uuid album) {
        this.userId = requireNonNull(userId, "userId");
        this.token = requireNonNull(token, "token");
        this.album = requireNonNull(album, "album");
        if (this.userId.isBlank()) {
            throw new IllegalArgumentException(this + ": No user id");
        }
        this.op = op;
    }

    private static LeasesRequest fromMap(
        Op op,
        String body,
        Function<? super String, ? extends Map<String, Object>> jsonParser
    ) {
        try {
            Function<String, String> f = lookup(body, jsonParser).andThen(String::valueOf);
            return new LeasesRequest(
                f.apply("userId"),
                Uuid.from(f.apply("token")),
                op,
                Uuid.from(f.apply("album"))
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read lease: " + body, e);
        }
    }

    private static Function<String, Object> lookup(
        String body, Function<? super String, ? extends Map<String, Object>> jsonParser
    ) {
        Map<String, Object> map = jsonParser.apply(body);
        return key ->
            map.getOrDefault(key, "");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + userId + "]";
    }
}
