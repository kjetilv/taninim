package taninim.kudu;

import module uplift.kernel;
import module uplift.uuid;

import static java.util.Objects.requireNonNull;

public record TrackRange(Track track, Range range, Uuid token) {

    public TrackRange {
        requireNonNull(track, "track");
        requireNonNull(range, "range");
        requireNonNull(token, "token");
    }
}
