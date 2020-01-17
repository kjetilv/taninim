package mediaserver.util;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Throwables {

    public static Stream<Throwable> causes(Throwable e) {

        return StreamSupport.stream(new Spliterators.AbstractSpliterator<Throwable>(
            Long.MAX_VALUE, Spliterator.IMMUTABLE
        ) {
            private Throwable t = e;

            @Override
            public boolean tryAdvance(Consumer<? super Throwable> action) {
                action.accept(t);
                Throwable cause = t.getCause();
                if (cause == null || cause == t) {
                    return false;
                }
                t = cause;
                return true;
            }
        }, false);
    }
}
