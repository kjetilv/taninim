package mediaserver.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static mediaserver.util.Sourced.Type.JAR;
import static mediaserver.util.Sourced.Type.SOURCES;

public final class Sourced<T> {

    private final Type source;

    private final T object;

    private final URL url;

    private static final String GRADLE_OUT = "out/production";

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

        return unpack().map(map);
    }

    public static <T> Sourced<T> notFound() {

        return new Sourced<>(Type.UNKNOWN, null, null);
    }

    public static Sourced<InputStream> readStream(String resource) {

        return url(resource)
            .map(sourceUrl ->
                isInSources(sourceUrl)
                    ? from(SOURCES, readSources(resource, sourceUrl), sourceUrl)
                    : from(JAR, readClasspath(resource), sourceUrl))
            .orElseGet(
                Sourced::notFound);
    }

    private static InputStream readClasspath(String resource) {

        return Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
    }

    private static InputStream readSources(String resource, URL url) {

        return fromSourceEnvironment(url).orElseThrow(() ->
            new IllegalArgumentException("No such resource: " + resource));
    }

    private static Optional<URL> url(String resource) {

        Optional<URL> url = Optional.ofNullable(
            Thread.currentThread().getContextClassLoader().getResource(resource));
        if (url.isEmpty()) {
            throw new IllegalArgumentException("No such resource: " + resource);
        }
        return url;
    }

    private static boolean isInSources(URL url) {

        return url.getFile().contains(GRADLE_OUT);
    }

    private static Optional<InputStream> fromSourceEnvironment(URL url) {

        return Optional.of(url)
            .map(URL::getFile)
            .map(name ->
                name.replaceAll(GRADLE_OUT, "src/main"))
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
}
