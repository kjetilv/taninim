package mediaserver.sessions;

import java.util.Objects;
import java.util.regex.Pattern;

public enum AccessLevel {
    NONE,
    LOGIN,
    STREAM_SINGLE,
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
        return valueOf(WS.matcher(accessLevel.toUpperCase()).replaceAll("_"));
    }

    private static final Pattern WS = Pattern.compile("\\s+");
}
