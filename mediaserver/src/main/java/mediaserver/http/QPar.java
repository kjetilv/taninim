package mediaserver.http;

import java.util.Arrays;
import java.util.Optional;

public enum QPar implements Par {

    playlist,

    curation,

    album,

    artist,

    track,

    series,

    streamlease,

    streamHighlighted,

    autoplay;

    @Override
    public String getName() {

        return name();
    }

    public static Optional<QPar> get(String substring) {

        return Arrays.stream(values())
            .filter(v ->
                v.name().equalsIgnoreCase(substring))
            .findFirst();
    }
}
