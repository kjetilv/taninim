package mediaserver.files;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Artist extends AbstractNameHashable {

    private static final long serialVersionUID = -6584400668175206925L;

    private Artist(String name) {

        super(name);
    }

    public static Artist get(String name) {

        return AbstractNameHashable.get(Artist::new, name);
    }

    public Collection<Artist> getCompositeArtists() {

        return split(getName(), "&")
            .flatMap(split -> split(split, ","))
            .flatMap(split -> split(split, "/"))
            .map(Artist::get)
            .collect(Collectors.toList());
    }

    private Stream<String> split(String name, String rex) {

        return (name.contains(rex) ? Arrays.stream(name.split(rex)) : Stream.of(name))
            .filter(s ->
                !s.isBlank())
            .map(String::trim);
    }

}
