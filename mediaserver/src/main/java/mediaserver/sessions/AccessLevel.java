package mediaserver.sessions;

import java.util.Objects;

public enum AccessLevel {

    NONE,

    LOGIN,

    STREAM_CURATED,

    STREAM,

    STREAM_PLAYLISTS,

    ADMIN;

    public boolean satisfies(AccessLevel level) {
        return ordinal() >= Objects.requireNonNull(level, "level").ordinal();
    }

    public String getDescription() {
        return name().toLowerCase().replace('_', ' ');
    }

    public static AccessLevel get(String accessLevel) {

        return valueOf(accessLevel
            .toUpperCase()
            .replaceAll("\\s+", "_"));
    }
}
