package taninim.yellin;

import com.github.kjetilv.uplift.kernel.http.QueryParams;
import com.github.kjetilv.uplift.kernel.io.CaseInsensitiveHashMap;
import com.github.kjetilv.uplift.kernel.uuid.Uuid;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public record LeasesRequest(
    String userId,
    Uuid token,
    Op op,
    Uuid album
) {


    public static LeasesRequest acquire(
        String body,
        Function<? super String, ? extends Map<?, ?>> jsonParser
    ) {
        return fromMap(Op.ACQUIRE, body, jsonParser);
    }

    public static LeasesRequest release(
        String body,
        Function<? super String, ? extends Map<?, ?>> jsonParser
    ) {
        return fromMap(Op.RELEASE, body, jsonParser);
    }

    public enum Op {

        ACQUIRE,
        RELEASE

    }

    public static Optional<LeasesRequest> release(String path) {
        QueryParams.Look look = QueryParams.get(path);
        return look.up("userId")
            .flatMap(userId ->
                look.up("album").flatMap(Uuid::maybeFrom)
                    .flatMap(album ->
                        look.up("token").flatMap(Uuid::maybeFrom)
                            .map(token ->
                                new LeasesRequest(userId, token, Op.RELEASE, album))));
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
        Function<? super String, ? extends Map<?, ?>> jsonParser
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

    private static LeasesRequest fromMap(
        Op op,
        Map<?, ?> map
    ) {
        try {
            Map<String, ?> wrap = CaseInsensitiveHashMap.wrap(map);
            Function<String, String> f = key -> wrap.get(key).toString();
            return new LeasesRequest(
                f.apply("userId"),
                Uuid.from(f.apply("token")),
                op,
                Uuid.from(f.apply("album"))
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read lease: " + map, e);
        }
    }

    private static Function<String, ?> lookup(String body, Function<? super String, ? extends Map<?, ?>> parser) {
        return parser.apply(body)::get;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + userId + "]";
    }
}
