package mediaserver.files;

import java.io.Serializable;
import java.net.URI;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Credits implements Serializable {

    private final Collection<Credit> credits;

    private static final long serialVersionUID = 3560105313561183479L;

    public Credits() {

        this(null);
    }

    private Credits(Collection<Credit> credits) {

        this.credits = credits == null || credits.isEmpty() ? Collections.emptyList() : List.copyOf(credits);
    }

    public Credits credit(
        String name,
        URI uri,
        String typeDescription
    ) {

        Optional<Credit.ExternalType> recognizedType =
            Arrays.stream(Credit.ExternalType.values())
                .filter(type ->
                    type.matches(typeDescription))
                .findFirst();
        Credit credit =
            new Credit(name, uri, typeDescription, recognizedType.orElse(null));
        if (credits.contains(credit)) {
            return this;
        }
        return new Credits(Stream.concat(
            credit.getCompositeCredits().stream(),
            credits.stream())
            .distinct()
            .collect(Collectors.toList()));
    }

    public Collection<Credit> getCredits() {

        return credits;
    }

    public Collection<Credit> getOtherCredits() {

        return credits(other());
    }

    public Collection<Credit> getArtistCredits() {

        return credits(other().negate());
    }

    public Collection<Credit> credits(Predicate<Credit> other) {

        List<Credit> collect = credits.stream().filter(other).collect(Collectors.toList());
        return collect;
    }

    public Predicate<Credit> other() {

        return credit -> credit.getExternalType() != null;
    }

    public Credits append(Credits albumContext) {

        List<Credit> allCredits = Stream.concat(
            credits.stream(),
            albumContext.credits.stream()
        ).distinct().collect(Collectors.toList());
        return new Credits(allCredits);
    }
}
