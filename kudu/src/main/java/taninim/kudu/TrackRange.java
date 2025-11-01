package taninim.kudu;

import com.github.kjetilv.uplift.kernel.io.Range;
import com.github.kjetilv.uplift.uuid.Uuid;

import static java.util.Objects.requireNonNull;

public record TrackRange(Track track, Range range, Uuid token) {

    public TrackRange {
        requireNonNull(track, "track");
        requireNonNull(range, "range");
        requireNonNull(token, "token");
    }
}
