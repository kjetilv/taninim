package mediaserver.util;

import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class Sourced<T> {

    private final IO.Type type;

    private final T object;

    private final URL url;

    Sourced(IO.Type type, T object, URL url) {
        this.type = Objects.requireNonNull(type, "source");
        this.object = object;
        this.url = url;
    }

    public <R> Sourced<R> map(Function<? super T, ? extends R> trans) {
        return map((type1, t) -> trans.apply(t));
    }

    public <R> Sourced<R> map(BiFunction<? super IO.Type, ? super T, ? extends R> trans) {
        return new Sourced<>(type, object == null ? null : trans.apply(type, object), url);
    }

    public <R> Optional<R> unpackTyped(IO.Type type, Function<? super T, ? extends R> action) {
        return type == this.type
            ? Optional.ofNullable(Objects.requireNonNull(action, "action").apply(object))
            : Optional.empty();
    }

    public IO.Type sourceType() {
        return type;
    }

    public URL getUrl() {
        return url;
    }

    public Optional<T> unpack() {
        return Optional.ofNullable(object);
    }

    public <R> Optional<R> unpack(Function<? super T, ? extends R> map) {
        return unpack().map(map);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type + ": " + url + "]";
    }
}
