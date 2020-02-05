package mediaserver.http;

import java.util.Arrays;
import java.util.Optional;

public enum FPar implements Par {

    JUKEBOX_ALBUM("jukeboxAlbum"),

    JUKEBOX_TRACK("jukeboxTrack"),

    JUKEBOX_CLEAR("jukeboxClear"),

    IDS,

    SESSION;

    private final String name;

    FPar() {

        this(null);
    }

    FPar(String name) {

        this.name = name == null ? this.name().toLowerCase() : name;
    }

    @Override
    public String getName() {

        return name;
    }

    public static Optional<FPar> get(String substring) {

        return Arrays.stream(values())
            .filter(v ->
                v.name().equalsIgnoreCase(substring))
            .findFirst();
    }
}
