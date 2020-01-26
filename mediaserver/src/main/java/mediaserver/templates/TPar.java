package mediaserver.templates;

import java.util.Arrays;
import java.util.Optional;

public enum TPar {

    MEDIA,

    PLYR,

    PLAYLIST,

    PLAYLISTS,

    CURATION,

    CURATIONS,

    ALBUM,

    ARTIST,

    SERIES,

    PLAY_TRACK("playTrack"),

    AUTOPLAY,

    NEXT_TRACK("nextTrack"),

    PREVIOUS_TRACK("previousTrack"),

    PLAY_TRACKS("playTracks"),

    USER,

    HOST,

    PROTOCOL,

    IDS,

    SESSIONS,

    STREAMLEASE,

    EXCHANGES;

    private final String name;

    TPar() {
        this(null);
    }
    TPar(String name) {
        this.name = name == null ? this.name().toLowerCase() : name;
    }

    public String getName() {
        return name;
    }

    public static Optional<TPar> get(String substring) {
        return Arrays.stream(values())
            .filter(v ->
                v.name().equalsIgnoreCase(substring))
            .findFirst();
    }
}
