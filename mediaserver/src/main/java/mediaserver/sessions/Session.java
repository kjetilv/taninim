package mediaserver.sessions;

import mediaserver.Config;
import mediaserver.externals.FacebookUser;
import mediaserver.util.Print;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class Session {

    private final Instant startTime;

    private final Instant sessionCutoff;

    private final Duration inactivityMax;

    private Instant lastAccessed;

    private final UUID cookie;

    private final FacebookUser facebookUser;

    private final AtomicLong bytesLeft = new AtomicLong(QUOTA);

    private static final int K = 1_024;

    private static final int M = K * K;

    private static final int QUOTA = Config.MEGAS_PER_SESSION * K * K;

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

    public Session streaming(long bytes) {

        bytesLeft.updateAndGet(remaining -> remaining - bytes);
        return this;
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
            " " + amount() + "/" + QUOTA / M + "mb" +
            " -" + Duration.between(lastAccessed, sessionCutoff).truncatedTo(ChronoUnit.MINUTES) +
            "]";
    }

    private String amount() {

        long left = QUOTA - bytesLeft.get();
        if (left > M) {
            return left / M + "mb";
        }
        if (left > K) {
            return left / K + "kb";
        }
        return left + "b";
    }
}
