package taninim.kudu;

import com.github.kjetilv.uplift.hash.Hash;
import com.github.kjetilv.uplift.hash.HashKind.K128;
import com.github.kjetilv.uplift.kernel.io.Range;

import static java.util.Objects.requireNonNull;

public record TrackRange(Track track, Range range, Hash<K128> token) {

    public TrackRange {
        requireNonNull(track, "track");
        requireNonNull(range, "range");
        requireNonNull(token, "token");
    }
}
