package mediaserver.sessions;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

import mediaserver.media.Album;
import mediaserver.media.Track;
import mediaserver.util.ExpiringState;
import mediaserver.util.Pair;

public class SessionState {

    private final ExpiringState<Pair<Album, Track>> randomTrack = new ExpiringState<>(Duration.ofHours(1));

    public Optional<Duration> getRandomTrackRemaining(Instant time) {

        return randomTrack.getRemaining(time);
    }

    public Optional<Pair<Album, Track>> getRandomTrack(Instant time) {

        return getRandomTrack(time, null);
    }

    public Optional<Pair<Album, Track>> getRandomTrack(
        Instant time, Supplier<Optional<Pair<Album, Track>>> newRandom) {

        return randomTrack.get(time, newRandom);
    }
}
