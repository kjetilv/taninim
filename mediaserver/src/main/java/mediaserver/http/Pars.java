package mediaserver.http;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class Pars<P extends Par<T, R>, T, R> {

    private final Map<P, Collection<R>> pars;

    Pars(Map<P, Collection<R>> pars) {

        this.pars = pars;
    }

    public Stream<String> get(P par) {

        return Optional.ofNullable(pars.get(par)).stream().flatMap(Collection::stream).map(String::valueOf);
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" +
            pars.entrySet().stream()
                .map(e ->
                    e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(" ")) +
            "]";
    }
}
