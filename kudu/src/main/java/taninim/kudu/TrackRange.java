package taninim.kudu;

import com.github.kjetilv.uplift.kernel.io.Range;
import com.github.kjetilv.uplift.uuid.Uuid;

import static java.util.Objects.requireNonNull;

public record TrackRange(Track track, Range range, Uuid token) {

    public TrackRange(Track track, Range range, Uuid token) {
        this.track = requireNonNull(track, "track");
        this.range = requireNonNull(range, "range");
        this.token = requireNonNull(token, "token");
    }

    public String format() {
        return track.format().suffix();
    }

    public Uuid trackUUID() {
        return track.trackUUID();
    }

    public String file() {
        return track.file();
    }
}

