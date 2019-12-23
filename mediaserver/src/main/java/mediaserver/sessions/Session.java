package mediaserver.sessions;

import mediaserver.externals.FacebookUser;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Session {

    private final Instant startTime;

    private final Instant cutoff;

    private final Duration inactivityMax;

    private Instant lastAccessed;

    private final UUID cookie;

    private final FacebookUser facebookUser;

    public Session(FacebookUser facebookUser, UUID cookie, Instant startTime, Instant cutoff, Duration inactivityMax) {

        this.facebookUser = Objects.requireNonNull(facebookUser, "facebookUser");
        this.cookie = Objects.requireNonNull(cookie, "cookie");
        this.startTime = Objects.requireNonNull(startTime, "startTime");
        this.lastAccessed = this.startTime;
        this.cutoff = Objects.requireNonNull(cutoff, "cutoff");
        this.inactivityMax = Objects.requireNonNull(inactivityMax, "inactivityMax");
    }

    public UUID getCookie() {

        return cookie;
    }

    public FacebookUser getFacebookUser() {

        return facebookUser;
    }

    public boolean expiredAt(Instant currentTime) {

        if (currentTime.isAfter(cutoff)) {
            return true;
        }
        Duration inactivity = Duration.between(lastAccessed, currentTime);
        return inactivity.toSeconds() > inactivityMax.toSeconds();
    }

    public Session accessedAt(Instant currentTime) {

        lastAccessed = currentTime;
        return this;
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() +
            "[[" + startTime + "] " + facebookUser + " -> " + cutoff + " / " + cookie + "]";
    }
}
