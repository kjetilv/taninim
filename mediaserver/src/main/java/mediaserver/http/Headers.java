package mediaserver.http;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.annotation.Nonnull;

public interface Headers extends Consumer<Headers.Setter> {

    interface Setter extends BiConsumer<CharSequence, CharSequence> {

        void set(CharSequence key, CharSequence value);

        @Override
        default void accept(CharSequence key, CharSequence value) {
            set(key, value);
        }
    }

    @Override
    default void accept(Setter setter) {
        set(setter);
    }

    default Headers and(Headers headers) {
        return andThen(headers);
    }

    @Nonnull
    @Override
    default Headers andThen(@Nonnull Consumer<? super Setter> after) {
        return setter -> {
            this.accept(setter);
            after.accept(setter);
        };
    }

    void set(Setter setter);
}
