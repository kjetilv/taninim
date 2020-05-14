package mediaserver.util;

import java.util.Objects;

public class AbstractPair<T1, T2> {

    private final T1 t1;

    private final T2 t2;

    protected AbstractPair(T1 t1, T2 t2) {

        this.t1 = t1;
        this.t2 = t2;
    }

    public T1 getT1() {

        return t1;
    }

    public T2 getT2() {

        return t2;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        return o != null && getClass() == o.getClass()
            && Objects.equals(t1, ((AbstractPair<?, ?>) o).t1)
            && Objects.equals(t2, ((AbstractPair<?, ?>) o).t2);
    }

    @Override
    public int hashCode() {

        return Objects.hash(t1, t2);
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + t1 + " " + t2 + "]";
    }
}
