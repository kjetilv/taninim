package mediaserver.http;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

public enum FPar implements Par<Req, String> {
    jukeboxAlbum,
    jukeboxTrack,
    jukeboxClear,
    updatedIds,
    session,
    ids;

    public static Optional<FPar> get(String substring) {
        return Arrays.stream(values())
            .filter(v ->
                v.name().equalsIgnoreCase(substring))
            .findFirst();
    }

    @Override
    public Stream<String> params(Req req) {
        return req.getFormParameters().get(this);
    }
}
