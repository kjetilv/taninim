package mediaserver.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class Pairs {

    private Pairs() {

    }

    public static <T> Collection<Pair<T, T>> pairs(Collection<T> t, T end) {

        ArrayList<T> l = new ArrayList<>(t);
        int size = l.size();
        return IntStream.range(0, size)
            .mapToObj(i ->
                Pair.of(
                    l.get(i),
                    i + 1 < size
                        ? l.get(i + 1)
                        : end))
            .collect(Collectors.toList());
    }
}
