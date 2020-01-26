package mediaserver.sessions;

import mediaserver.externals.FbUser;
import mediaserver.http.Req;
import mediaserver.util.Print;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
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

    private final FbUser fbUser;

    private final AtomicReference<Instant> lastAccessed = new AtomicReference<>();

    private final AtomicLong bytesStreamed = new AtomicLong();

    public Session(
        FbUser fbUser,
        UUID cookie,
        Instant startTime,
        Instant sessionCutoff,
        Duration inactivityMax,
        AccessLevel accessLevel,
        long bytesQuota
    ) {

        this.fbUser = Objects.requireNonNull(fbUser, "fbUser");
        this.cookie = Objects.requireNonNull(cookie, "cookie");
        this.startTime = Objects.requireNonNull(startTime, "startTime").truncatedTo(ChronoUnit.SECONDS);
        this.accessLevel = Objects.requireNonNull(accessLevel, "accessLevel");
        this.bytesQuota = bytesQuota;
        this.lastAccessed.set(this.startTime);
        this.sessionCutoff = Objects.requireNonNull(sessionCutoff, "cutoff").truncatedTo(ChronoUnit.SECONDS);
        this.inactivityMax = Objects.requireNonNull(inactivityMax, "inactivityMax");
    }

    public Instant getStartTime() {

        return startTime;
    }

    public UUID getCookie() {

        return cookie;
    }

    public User getActiveUser(Req req) {

        return new User(this, req.getTime(), fbUser.getName(), fbUser.getId());
    }

    public FbUser getFbUser() {

        return fbUser;
    }

    public Session streaming(long bytes) {

        bytesStreamed.addAndGet(bytes);
        return this;
    }

    public boolean isPrivileged() {

        return false;
    }

    public Session accessedBy(Req req) {

        lastAccessed.set(req.getTime());
        return this;
    }

    public String getCurrentStatus() {

        return getCurrentStatus(Instant.now());
    }

    public String getCurrentStatus(Instant time) {

        return getStatus(time).stream().distinct()
            .map(Status::getDescription)
            .collect(Collectors.joining(", "));
    }

    public Collection<Status> getStatus(Instant time) {

        return new HashSet<>(Arrays.asList(
            sessionStatus(time),
            activityStatus(time),
            quotaStatus()));
    }

    public boolean isValid(Instant time) {

        Collection<Session.Status> statuses = getStatus(time);
        return statuses.size() == 1 && statuses.iterator().next() == Status.OK;
    }

    public boolean hasLevel(AccessLevel accessLevel) {

        return this.accessLevel.satisfies(accessLevel);
    }

    public AccessLevel getAccessLevel() {

        return accessLevel;
    }

    public String getDescription() {

        return toString();
    }

    public Temporal getEndTime() {

        return sessionCutoff;
    }

    public long getStreamedBytes() {

        return bytesStreamed.get();
    }

    public long getStreamQuota() {

        return bytesQuota;
    }

    private Status sessionStatus(Instant time) {

        return time.isAfter(sessionCutoff)
            ? Status.SESSION_LENGTH_CUTOFF
            : Status.OK;
    }

    private Status activityStatus(Instant time) {

        Duration inactivity =
            Duration.between(lastAccessed.get(), time);
        return inactivity.toSeconds() > inactivityMax.toSeconds()
            ? Status.INACTIVITY_CUTOFF
            : Status.OK;
    }

    private Status quotaStatus() {

        return bytesStreamed.get() < bytesQuota
            ? Status.OK
            : Status.QUOTA_EXCEEDED;
    }

    public enum Status {

        OK,

        SESSION_LENGTH_CUTOFF,

        INACTIVITY_CUTOFF,

        QUOTA_EXCEEDED;

        public String getDescription() {

            return name().toLowerCase().replace('_', ' ');
        }
    }

    @Override
    public int hashCode() {

        return Objects.hash(cookie, fbUser);
    }

    @Override
    public boolean equals(Object o) {

        return this == o ||
            o instanceof Session &&
                Objects.equals(cookie, ((Session) o).cookie) &&
                Objects.equals(fbUser, ((Session) o).fbUser);
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + fbUser + "/" + accessLevel + "/" + Print.uuid(cookie) +
            "@" + Print.aboutTime(startTime) +
            " s:" + Print.bytes(bytesStreamed.get()) + "/" + Print.bytes(bytesQuota) +
            " t:" + Duration.between(lastAccessed.get(), sessionCutoff).truncatedTo(ChronoUnit.MINUTES) +
            "]";
    }
}
