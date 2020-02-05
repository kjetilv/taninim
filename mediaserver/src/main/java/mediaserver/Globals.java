package mediaserver;

import mediaserver.http.Req;
import mediaserver.media.Album;
import mediaserver.media.Track;
import mediaserver.util.ExpiringState;
import mediaserver.util.P2;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public final class Globals {

    private static final Globals INSTANCE = new Globals();

    private final ExpiringState<P2<Album, Track>> globalTrack = new ExpiringState<>(Duration.ofDays(1));

    private Globals() {
    }

    public static Globals get() {

        return INSTANCE;
    }

    public Optional<P2<Album, Track>> getGlobalTrack(Instant time) {

        return globalTrack.get(time);
    }

    public void unsetGlobalTrack() {
        globalTrack.expire();
    }

    public void setGlobalTrack(Instant time, P2<Album, Track> global) {

        globalTrack.set(time, global);
    }

    public static Optional<P2<Album, Track>> globalTrack(Req req) {

        return get().getGlobalTrack(req.getTime());
    }

}
