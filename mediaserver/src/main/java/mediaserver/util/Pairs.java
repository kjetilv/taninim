package mediaserver.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Pairs {

    public static <T> Collection<Pair<T>> pairs(Collection<T> t, T end) {

        ArrayList<T> l = new ArrayList<>(t);
        return IntStream.range(0, l.size())
            .mapToObj(i ->
                new Pair<>(
                    l.get(i),
                    i + 1 < l.size() ? l.get(i + 1) : end))
            .collect(Collectors.toList());
    }
}
