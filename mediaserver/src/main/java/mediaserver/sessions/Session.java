package mediaserver.sessions;

import mediaserver.externals.FacebookUser;
import mediaserver.util.Print;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class Session {

    public enum Status {

        OK,

        SESSION_LENGTH_CUTOFF,

        INACTIVITY_CUTOFF,

        QUOTA_EXCEEDED
    }

    private final Instant startTime;

    private final long bytesQuota;

    private final Instant sessionCutoff;

    private final Duration inactivityMax;

    private final UUID cookie;

    private final FacebookUser facebookUser;

    private final AtomicReference<Instant> lastAccessed = new AtomicReference<>();

    private final AtomicLong bytesStreamed = new AtomicLong();

    private final AtomicReference<Status> status = new AtomicReference<>(Status.OK);

    public Session(
        FacebookUser facebookUser,
        UUID cookie,
        Instant startTime,
        Instant sessionCutoff,
        Duration inactivityMax,
        long bytesQuota
    ) {

        this.facebookUser = Objects.requireNonNull(facebookUser, "facebookUser");
        this.cookie = Objects.requireNonNull(cookie, "cookie");
        this.startTime = Objects.requireNonNull(startTime, "startTime");
        this.bytesQuota = bytesQuota;
        this.lastAccessed.set(this.startTime);
        this.sessionCutoff = Objects.requireNonNull(sessionCutoff, "cutoff");
        this.inactivityMax = Objects.requireNonNull(inactivityMax, "inactivityMax");
    }

    public UUID getCookie() {

        return cookie;
    }

    public FacebookUser getFacebookUser() {

        return facebookUser;
    }

    public Status stillActive(Instant currentTime) {

        if (currentTime.isAfter(sessionCutoff)) {
            return Status.SESSION_LENGTH_CUTOFF;
        }
        Duration inactivity = Duration.between(lastAccessed.get(), currentTime);
        return inactivity.toSeconds() > inactivityMax.toSeconds()
            ? Status.INACTIVITY_CUTOFF
            : Status.OK;
    }

    public Status withinQuota() {

        return bytesStreamed.get() < bytesQuota
            ? Status.OK
            : Status.QUOTA_EXCEEDED;
    }

    public Session streaming(long bytes) {

        bytesStreamed.updateAndGet(remaining -> remaining - bytes);
        return this;
    }

    public boolean isPrivileged() {

        return facebookUser.getId().equals("2787973921215833");
    }

    public Session accessedAt(Instant currentTime) {

        lastAccessed.set(currentTime);
        return this;
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + facebookUser + "/" + cookie +
            "@" + Print.aboutTime(startTime) +
            " s:" + Print.bytes(bytesStreamed.get()) + "/" + Print.bytes(bytesQuota) +
            " t:" + Duration.between(lastAccessed.get(), sessionCutoff).truncatedTo(ChronoUnit.MINUTES) +
            "]";
    }
}
