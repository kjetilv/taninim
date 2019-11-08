package mediaserver.gui;

import java.util.Arrays;
import java.util.Optional;

public enum QPar {

    MEDIA,

    PLAYLIST,

    ALBUM,

    ARTIST,

    TRACK,

    SERIES,

    CATEGORY,

    PLAY_TRACK("playTrack"),

    HOST;

    private final String name;

    QPar() {
        this(null);
    }
    QPar(String name) {
        this.name = name == null ? this.name().toLowerCase() : name;
    }

    public String getName() {
        return name;
    }

    public static Optional<QPar> get(String substring) {
        return Arrays.stream(values())
            .filter(v ->
                v.name().equalsIgnoreCase(substring))
            .findFirst();
    }
}
