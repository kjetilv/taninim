package mediaserver.util;

import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public final class Sourced<T> {

    private final Type source;

    private final T object;

    private final URL url;

    private Sourced(Type source, T object, URL url) {

        this.source = Objects.requireNonNull(source, "source");
        this.object = object;
        this.url = url;
    }

    public static <T> Sourced<T> from(Type source, T t, URL url) {

        return new Sourced<>(source, t, url);
    }

    public static <T> Function<T, Sourced<T>> from(Type source, URL url) {

        return t -> new Sourced<>(source, t, url);
    }

    public <R> Sourced<R> map(Function<T, R> trans) {

        return new Sourced<>(source, object == null ? null : trans.apply(object), url);
    }

    public void ifType(Type type, Runnable action) {

        if (type == this.source) {
            action.run();
        }
    }

    public <R> Optional<R> unpackTyped(Type type, Function<T, R> action) {

        if (type == this.source) {
            return Optional.ofNullable(source == null ? null : action.apply(object));
        }
        return Optional.empty();
    }

    public Type source() {

        return source;
    }

    public URL getUrl() {

        return url;
    }

    public Optional<T> unpack() {

        return Optional.ofNullable(object);
    }

    public <R> Optional<R> unpack(Function<T, R> map) {

        return Optional.ofNullable(object).map(map);
    }

    public static <T> Sourced<T> notFound() {

        return new Sourced<>(Type.UNKNOWN, null, null);
    }

    public enum Type {

        SOURCES, JAR, UNKNOWN
    }
}
