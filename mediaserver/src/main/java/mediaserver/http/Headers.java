package mediaserver.http;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
    
    @Override
    default Headers andThen(Consumer<? super Setter> after) {
        return setter -> {
            this.accept(setter);
            after.accept(setter);
        };
    }
    
    default Headers and(Headers headers) {
        return andThen(headers);
    }
    
    void set(Setter setter);
}
