package mediaserver.http;

import java.util.Arrays;
import java.util.Optional;

public enum FPar implements Par {

    jukeboxAlbum,

    jukeboxTrack,

    jukeboxClear,

    updatedIds,

    session;

    @Override
    public String getName() {

        return name();
    }

    public static Optional<FPar> get(String substring) {

        return Arrays.stream(values())
            .filter(v ->
                v.name().equalsIgnoreCase(substring))
            .findFirst();
    }
}
