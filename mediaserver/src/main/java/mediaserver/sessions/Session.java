package mediaserver.sessions;

import mediaserver.externals.FacebookUser;
import mediaserver.util.Print;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

public final class Session {

    private final Instant startTime;

    private final Instant sessionCutoff;

    private final Duration inactivityMax;

    private Instant lastAccessed;

    private final UUID cookie;

    private final FacebookUser facebookUser;

    public Session(
        FacebookUser facebookUser,
        UUID cookie,
        Instant startTime,
        Instant sessionCutoff,
        Duration inactivityMax
    ) {

        this.facebookUser = Objects.requireNonNull(facebookUser, "facebookUser");
        this.cookie = Objects.requireNonNull(cookie, "cookie");
        this.startTime = Objects.requireNonNull(startTime, "startTime");
        this.lastAccessed = this.startTime;
        this.sessionCutoff = Objects.requireNonNull(sessionCutoff, "cutoff");
        this.inactivityMax = Objects.requireNonNull(inactivityMax, "inactivityMax");
    }

    public UUID getCookie() {

        return cookie;
    }

    public FacebookUser getFacebookUser() {

        return facebookUser;
    }

    public boolean expiredAt(Instant currentTime) {

        if (currentTime.isAfter(sessionCutoff)) {
            return true;
        }
        Duration inactivity = Duration.between(lastAccessed, currentTime);
        return inactivity.toSeconds() > inactivityMax.toSeconds();
    }

    public Session accessedAt(Instant currentTime) {

        lastAccessed = currentTime;
        return this;
    }

    public boolean isPrivileged() {

        return facebookUser.getId().equals("2787973921215833");
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + facebookUser +
            " @ " + Print.aboutTime(startTime) +
            ", -" + Duration.between(startTime, sessionCutoff).truncatedTo(ChronoUnit.MINUTES) +
            "]";
    }
}
