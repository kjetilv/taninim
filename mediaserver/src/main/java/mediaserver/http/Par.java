package mediaserver.http;

import java.util.UUID;
import java.util.stream.Stream;

public interface Par<T, R> {

    String getName();

    default boolean isFalse(T source) {
        return !isTrue(source);
    }

    default boolean isTrue(T source) {
        return params(source).map(String::valueOf).anyMatch(Boolean::parseBoolean);
    }

    default Stream<String> get(T source) {
        return params(source).map(String::valueOf);
    }

    default Stream<UUID> id(T source) {
        return get(source).map(UUID::fromString);
    }

    Stream<R> params(T t);
}
