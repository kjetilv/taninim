package mediaserver.media;

import mediaserver.util.DAC;

import java.io.Serializable;
import java.net.URI;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Credits implements Serializable {

    private final Collection<Credit> credits;

    private static final long serialVersionUID = 3560105313561183479L;

    public Credits() {

        this(null);
    }

    private Credits(Collection<Credit> credits) {

        this.credits = credits == null || credits.isEmpty() ? Collections.emptyList() : List.copyOf(credits);
    }

    public Collection<Credit> getCredits() {

        return credits;
    }

    @DAC
    public Collection<Credit> getOtherCredits() {

        return credits(other());
    }

    @DAC
    public Collection<Credit> getArtistCredits() {

        return withoutRedundants(credits(other().negate()));
    }

    public Credits append(Credits albumContext) {

        List<Credit> allCredits = Stream.concat(
            credits.stream(),
            albumContext.credits.stream()
        ).distinct().collect(Collectors.toList());
        return new Credits(allCredits);
    }

    Credits credit(String name, URI uri, String typeDescription) {

        Optional<Credit.ExternalType> recognizedType =
            Arrays.stream(Credit.ExternalType.values())
                .filter(type ->
                    type.matches(typeDescription))
                .findFirst();
        Credit credit =
            new Credit(name, uri, typeDescription, recognizedType.orElse(null));

        if (credits.containsAll(credit.getCompositeCredits())) {
            return this;
        }

        return new Credits(Stream.concat(
            credit.getCompositeCredits().stream(),
            credits.stream()
        ).distinct().collect(Collectors.toList()));
    }

    private Collection<Credit> credits(Predicate<Credit> other) {

        return this.credits.stream().filter(other).collect(Collectors.toList());
    }

    private static Collection<Credit> withoutRedundants(Collection<Credit> credits) {

        Collection<Artist> empty = credits.stream()
            .filter(Credit::isEmpty)
            .map(Credit::getArtist).collect(Collectors.toSet());
        return credits.stream()
            .filter(c ->
                !(c.isEmpty() && empty.contains(c.getArtist())))
            .collect(Collectors.toList());
    }

    private static Predicate<Credit> other() {

        return credit -> credit.getExternalType() != null;
    }
}
