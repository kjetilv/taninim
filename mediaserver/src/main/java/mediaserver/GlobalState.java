package mediaserver;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import mediaserver.media.Album;
import mediaserver.media.Track;
import mediaserver.util.ExpiringState;
import mediaserver.util.Pair;

@SuppressWarnings("UnusedReturnValue")
public final class GlobalState {

    public static GlobalState get() {

        return INSTANCE;
    }

    private final ExpiringState<Pair<Album, Track>> globalTrack = new ExpiringState<>(Duration.ofDays(1));

    private GlobalState() {

    }

    public boolean unsetGlobalTrack() {

        return globalTrack.expire();
    }

    public Optional<Pair<Album, Track>> getGlobalTrack(Instant time) {

        return globalTrack.get(time);
    }

    public Optional<Duration> getGlobalTrackRemaining(Instant time) {

        return globalTrack.getRemaining(time);
    }

    public boolean setGlobalTrack(Instant time, Album album, Track track) {

        return globalTrack.set(time, Pair.of(album, track), true);
    }

    private static final GlobalState INSTANCE = new GlobalState();
}
