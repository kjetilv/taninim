package mediaserver.util;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public final class Sourced<T> {

    private final Type source;

    private final T object;

    public static <T> Sourced<T> from(Type source, T t) {
        return new Sourced<>(source, t);
    }

    public static <T> Function<T, Sourced<T>> from(Type source) {
        return t -> new Sourced<>(source, t);
    }

    private Sourced(Type source, T object) {
        this.source = Objects.requireNonNull(source, "source");
        this.object = Objects.requireNonNull(object, "object");
    }

    public <R> Sourced<R> map(Function<T, R> trans) {
        return new Sourced<>(source, object == null ? null : trans.apply(object));
    }

    public Type source() {

        return source;
    }

    public Optional<T> unpack() {

        return Optional.ofNullable(object);
    }

    public <R> Optional<R> unpack(Function<T, R> map) {

        return Optional.ofNullable(object).map(map);
    }

    public enum Type {

        SOURCES, JAR
    }
}
