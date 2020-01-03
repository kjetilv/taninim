package mediaserver.sessions;

import mediaserver.externals.FacebookUser;
import mediaserver.gui.ActiveUser;
import mediaserver.http.WebPath;
import mediaserver.util.Print;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public final class Session {

    private final Instant startTime;

    private final AccessLevel accessLevel;

    private final long bytesQuota;

    private final Instant sessionCutoff;

    private final Duration inactivityMax;

    private final UUID cookie;

    private final FacebookUser facebookUser;

    private final AtomicReference<Instant> lastAccessed = new AtomicReference<>();

    private final AtomicLong bytesStreamed = new AtomicLong();

    public Session(
        FacebookUser facebookUser,
        UUID cookie,
        Instant startTime,
        Instant sessionCutoff,
        Duration inactivityMax,
        AccessLevel accessLevel,
        long bytesQuota
    ) {

        this.facebookUser = Objects.requireNonNull(facebookUser, "facebookUser");
        this.cookie = Objects.requireNonNull(cookie, "cookie");
        this.startTime = Objects.requireNonNull(startTime, "startTime");
        this.accessLevel = Objects.requireNonNull(accessLevel, "accessLevel");
        this.bytesQuota = bytesQuota;
        this.lastAccessed.set(this.startTime);
        this.sessionCutoff = Objects.requireNonNull(sessionCutoff, "cutoff");
        this.inactivityMax = Objects.requireNonNull(inactivityMax, "inactivityMax");
    }

    public Instant getStartTime() {

        return startTime;
    }

    public UUID getCookie() {

        return cookie;
    }

    public ActiveUser getActiveUser() {

        return new ActiveUser(facebookUser.getName(), accessLevel);
    }

    public FacebookUser getFacebookUser() {

        return facebookUser;
    }

    public Session streaming(long bytes) {

        bytesStreamed.addAndGet(bytes);
        return this;
    }

    public boolean isPrivileged() {

        return false;
    }

    public Session accessedBy(WebPath webPath) {

        lastAccessed.set(webPath.getTime());
        return this;
    }

    public Collection<Status> getCurrentStatus() {

        return status(null).stream().distinct().collect(Collectors.toList());
    }

    public Collection<Status> status(WebPath webPath) {

        return new HashSet<>(Arrays.asList(
            sessionStatus(webPath),
            activityStatus(webPath),
            quotaStatus()));
    }

    public boolean hasLevel(AccessLevel accessLevel) {

        return this.accessLevel.is(accessLevel);
    }

    public AccessLevel getAccessLevel() {

        return accessLevel;
    }

    public String getDescription() {
        return toString();
    }

    private Status sessionStatus(WebPath webPath) {

        return time(webPath).isAfter(sessionCutoff)
            ? Status.SESSION_LENGTH_CUTOFF
            : Status.OK;
    }

    private Status activityStatus(WebPath webPath) {

        Duration inactivity =
            Duration.between(lastAccessed.get(), time(webPath));
        return inactivity.toSeconds() > inactivityMax.toSeconds()
            ? Status.INACTIVITY_CUTOFF
            : Status.OK;
    }

    private Status quotaStatus() {

        return bytesStreamed.get() < bytesQuota
            ? Status.OK
            : Status.QUOTA_EXCEEDED;
    }

    private Instant time(WebPath webPath) {

        return webPath == null ? Instant.now() : webPath.getTime();
    }

    public enum Status {

        OK,

        SESSION_LENGTH_CUTOFF,

        INACTIVITY_CUTOFF,

        QUOTA_EXCEEDED
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + facebookUser + "/" + accessLevel + "/" + cookie +
            "@" + Print.aboutTime(startTime) +
            " s:" + Print.bytes(bytesStreamed.get()) + "/" + Print.bytes(bytesQuota) +
            " t:" + Duration.between(lastAccessed.get(), sessionCutoff).truncatedTo(ChronoUnit.MINUTES) +
            "]";
    }
}
