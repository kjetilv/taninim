package mediaserver.util;

import java.util.Map;
import java.util.function.Function;

@SuppressWarnings("unused")
public final class Pair<T1, T2> extends AbstractPair<T1, T2> {

    public static <T1, T2> Pair<T1, T2> of(Map.Entry<? extends T1, ? extends T2> e) {
        return of(e.getKey(), e.getValue());
    }

    public static <T1, T2> Pair<T1, T2> of(T1 t1, T2 t2) {
        return new Pair<>(t1, t2);
    }

    private Pair(T1 t1, T2 t2) {
        super(t1, t2);
    }

    public <T0> Pair<T0, T2> map1(Function<? super T1, ? extends T0> fun) {
        return Pair.of(fun.apply(getT1()), getT2());
    }

    public <T3> Pair<T1, T3> map2(Function<? super T2, ? extends T3> fun) {
        return Pair.of(getT1(), fun.apply(getT2()));
    }

    public Pair<T2, T1> flip() {
        return Pair.of(getT2(), getT1());
    }
}
