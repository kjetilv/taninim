package mediaserver.files;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Credits {

    private final Collection<Credit> credits;

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

        Optional<Credit.ExternalType> recognizedType = Arrays.stream(Credit.ExternalType.values())
            .filter(type ->
                type.matches(typeDescription))
            .findFirst();
        Credit credit = recognizedType
            .map(rec ->
                new Credit(name, uri, typeDescription, rec))
            .orElseGet(() ->
                new Credit(name, uri, typeDescription, null));
        return credits.contains(credit) ? this : new Credits(
            Stream.concat(
                Stream.of(credit),
                credits.stream()
            ).collect(Collectors.toSet()));
    }

    public Collection<Credit> getCredits() {

        return credits;
    }

    public Credits append(Credits albumContext) {

        List<Credit> allCredits = Stream.concat(
            credits.stream(),
            albumContext.credits.stream()
        ).distinct().collect(Collectors.toList());
        return new Credits(allCredits);
    }
}
