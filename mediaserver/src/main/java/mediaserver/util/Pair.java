package mediaserver.util;

import java.util.Map;

public final class Pair<T1, T2> extends AbstractPair<T1, T2> {

    private Pair(T1 t1, T2 t2) {

        super(t1, t2);
    }

    public static <T1, T2> Pair<T1, T2> of(Map.Entry<T1, T2> e) {

        return of(e.getKey(), e.getValue());
    }

    public static <T1, T2> Pair<T1, T2> of(T1 t1, T2 t2) {

        return new Pair<>(t1, t2);
    }
}
