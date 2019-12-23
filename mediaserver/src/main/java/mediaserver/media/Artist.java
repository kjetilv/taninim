package mediaserver.media;

import mediaserver.hash.AbstractNameHashable;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Artist extends AbstractNameHashable {

    private static final long serialVersionUID = -6584400668175206925L;

    private Artist(String name) {

        super(name);
    }

    public static Artist get(String name) {

        return AbstractNameHashable.get(Artist::new, name);
    }

    public Collection<Artist> getCompositeArtists() {

        return s(getName(), "&")
            .flatMap(s -> s(s, ", And "))
            .flatMap(s -> s(s, ", and "))
            .flatMap(s -> s(s, " and "))
            .flatMap(s -> s(s, " And "))
            .flatMap(s -> s(s, ","))
            .flatMap(s -> s(s, "/"))
            .map(Artist::get)
            .collect(Collectors.toList());
    }

    private Stream<String> s(String name, String rex) {

        Stream<String> stringStream = name.contains(rex)
            ? Arrays.stream(name.split(rex))
            : Stream.of(name);
        return stringStream
            .filter(s ->
                !s.isBlank()).map(String::trim);
    }
}
