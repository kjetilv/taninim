package taninim.auth;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public sealed interface Authed<T> {

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static <T> Authed<T> resolve(Optional<T> optional) {
        return optional
            .map(Authed::authorized)
            .orElseGet(Authed::empty);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static <T> Authed<T> require(Optional<T> optional) {
        return optional
            .map(Authed::authorized)
            .orElseGet(Authed::unauthorized);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static <T> Authed<T> require(Optional<T> optional, Supplier<String> failure) {
        return optional
            .map(Authed::authorized)
            .orElseGet(() -> unauthorized(failure.get()));
    }

    static <T> Authed<T> authorized(T value) {
        return value == null ? empty() : new OK<>(value);
    }

    static <T> Authed<T> empty() {
        return new Empty<>();
    }

    static <T> Authed<T> unauthorized() {
        return unauthorized(null);
    }

    static <T> Authed<T> unauthorized(String reason) {
        return new Failed<>(reason == null ? "Unauthorized" : reason);
    }

    @SuppressWarnings("unchecked")
    default <R> Authed<R> flatMap(Function<? super T, Authed<R>> mapper) {
        return (Authed<R>) this;
    }

    @SuppressWarnings("unchecked")
    default <R> Authed<R> map(Function<? super T, R> mapper) {
        return (Authed<R>) this;
    }

    default Authed<T> filter(Predicate<T> test) {
        return this;
    }

    default Authed<T> filterOr(Predicate<T> test, Supplier<Authed<T>> supplier) {
        return Objects.requireNonNull(supplier, "supplier").get();
    }

    default T orElseGet(Supplier<T> supplier) {
        return supplier.get();
    }

    default T orElseThrow() {
        throw new IllegalStateException(this.toString());
    }

    default Optional<T> toOptional() {
        return Optional.empty();
    }

    record OK<T>(T value) implements Authed<T> {

        public OK {
            Objects.requireNonNull(value, "value");
        }

        @Override
        public <R> Authed<R> flatMap(Function<? super T, Authed<R>> mapper) {
            return mapper.apply(value);
        }

        @Override
        public Optional<T> toOptional() {
            return Optional.of(value);
        }

        @Override
        public <R> Authed<R> map(Function<? super T, R> mapper) {
            return authorized(mapper.apply(value));
        }

        @Override
        public Authed<T> filter(Predicate<T> test) {
            return test.test(value) ? this : empty();
        }

        @Override
        public Authed<T> filterOr(Predicate<T> test, Supplier<Authed<T>> supplier) {
            var sup = Objects.requireNonNull(supplier, "supplier");
            return test.test(value) ? this : sup.get();
        }

        @Override
        public T orElseGet(Supplier<T> supplier) {
            return value;
        }

        @Override
        public T orElseThrow() {
            return value;
        }
    }

    record Empty<T>() implements Authed<T> {
    }

    record Failed<T>(String reason) implements Authed<T> {
    }
}
