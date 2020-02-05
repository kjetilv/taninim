package mediaserver.templates;

import mediaserver.http.Par;

import java.util.Arrays;
import java.util.Optional;

public enum TPar implements Par {

    MEDIA,

    PLYR,

    PLAYLIST,

    PLAYLISTS,

    CURATION,

    CURATIONS,

    ALBUM,

    ARTIST,

    SERIES,

    SELECTED,

    RANDOM_ALBUMS("randomAlbums"),

    HIGHLIGHTED_ALBUM("highlightedAlbum"),

    HIGHLIGHTED,

    TRACK_HIGHLIGHTED("trackHighlighted"),

    HIGHLIGHTED_SELECTED("highlightedSelected"),

    PLAYABLE_GROUPS("playableGroups"),

    ADMIN,

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

    @Override
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
