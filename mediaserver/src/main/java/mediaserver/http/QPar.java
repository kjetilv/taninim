package mediaserver.http;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

public enum QPar implements Par<Req, String> {
    playlist,
    curation,
    album,
    artist,
    unartist,
    track,
    series,
    streamlease,
    streamHighlighted,
    autoplay,
    sort,
    union;

    public static Optional<QPar> get(String substring) {
        return Arrays.stream(values())
            .filter(v ->
                v.name().equalsIgnoreCase(substring))
            .findFirst();
    }

    @Override
    public Stream<String> params(Req req) {
        return req.getQueryParameters().get(this);
    }
}
