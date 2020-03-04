package mediaserver.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static mediaserver.util.Sourced.Type.JAR;
import static mediaserver.util.Sourced.Type.SOURCES;

public final class Sourced<T> {

    private final Type type;

    private final T object;

    private final URL url;

    private static final String GRADLE_OUT = "out/production";

    private static final String SRC_MAIN = "src/main";

    private Sourced(Type type, T object, URL url) {

        this.type = Objects.requireNonNull(type, "source");
        this.object = object;
        this.url = url;
    }

    public static <T> Function<T, Sourced<T>> from(Type source, URL url) {

        return t -> new Sourced<>(source, t, url);
    }

    public <R> Sourced<R> map(Function<T, R> trans) {

        return map((type1, t) -> trans.apply(t));
    }

    public <R> Sourced<R> map(BiFunction<Type, T, R> trans) {

        return new Sourced<>(type, object == null ? null : trans.apply(type, object), url);
    }

    public Sourced<T> sourcesRefreshed(Supplier<T> supplier) {

        if (this.type == SOURCES) {
            return Sourced.from(SOURCES, supplier.get(), url);
        }
        return this;
    }

    public <R> Optional<R> unpackTyped(Type type, Function<T, R> action) {

        return type == this.type
            ? Optional.ofNullable(Objects.requireNonNull(action, "action").apply(object))
            : Optional.empty();
    }

    public Type sourceType() {

        return type;
    }

    public URL getUrl() {

        return url;
    }

    public Optional<T> unpack() {

        return Optional.ofNullable(object);
    }

    public <R> Optional<R> unpack(Function<T, R> map) {

        return unpack().map(map);
    }

    static Sourced<InputStream> readStream(String resource) {

        URL sourceUrl = Optional.ofNullable(
            Thread.currentThread().getContextClassLoader().getResource(resource)).orElseThrow(() ->
            new IllegalArgumentException("No such resource: " + resource));
        if (isInSources(sourceUrl)) {
            URL adjusted = inSources(sourceUrl);
            return from(SOURCES, readSources(resource, adjusted), adjusted);
        }
        return from(JAR, readClasspath(resource), sourceUrl);
    }

    private static <T> Sourced<T> from(Type source, T t, URL url) {

        return new Sourced<>(source, t, url);
    }

    private static URL inSources(URL sourceUrl) {

        try {
            return URI.create(sourceUrl.toExternalForm().replaceAll(GRADLE_OUT, SRC_MAIN)).toURL();
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Could not transform to sources url: " + sourceUrl, e);
        }
    }

    private static InputStream readClasspath(String resource) {

        return Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
    }

    private static InputStream readSources(String resource, URL url) {

        return fromSourceEnvironment(url).orElseThrow(() ->
            new IllegalArgumentException("No such resource: " + resource));
    }

    private static boolean isInSources(URL url) {

        return url.getFile().contains(GRADLE_OUT);
    }

    private static Optional<InputStream> fromSourceEnvironment(URL url) {

        return Optional.of(url)
            .map(URL::getFile)
            .map(name ->
                name.replaceAll(GRADLE_OUT, SRC_MAIN))
            .map(name -> {
                try {
                    return new FileInputStream(name);
                } catch (FileNotFoundException e) {
                    throw new IllegalStateException("Failed to open file: " + name, e);
                }
            });
    }

    public enum Type {

        SOURCES, JAR, UNKNOWN
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + type + ": " + url + "]";
    }
}
