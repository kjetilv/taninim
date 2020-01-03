package mediaserver.sessions;

import java.util.Objects;

public enum AccessLevel {

    NONE,

    LOGIN,

    STREAM_CURATED,

    STREAM,

    ADMIN;

    public boolean is(AccessLevel level) {
        return ordinal() >= Objects.requireNonNull(level, "level").ordinal();
    }
}
