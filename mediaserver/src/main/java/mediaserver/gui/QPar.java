package mediaserver.gui;

import java.util.Optional;

public enum QPar {

    MEDIA,

    PLAYLIST,

    ALBUM,

    ARTIST,

    TRACK,

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
        switch (substring.toLowerCase()) {
            case "album":
                return Optional.of(ALBUM);
            case "artist":
                return Optional.of(ARTIST);
            case "track":
                return Optional.of(TRACK);
            case "playtrack":
                return Optional.of(PLAY_TRACK);
            default:
                return Optional.empty();
        }
    }
}
