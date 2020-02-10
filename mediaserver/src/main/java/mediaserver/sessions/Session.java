package mediaserver.sessions;

import mediaserver.externals.FbUser;
import mediaserver.http.Req;
import mediaserver.media.Album;
import mediaserver.media.Track;
import mediaserver.util.ExpiringState;
import mediaserver.util.Pair;
import mediaserver.util.Print;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Session {

    private static final Logger log = LoggerFactory.getLogger(Session.class);

    private final Instant startTime;

    private final AccessLevel accessLevel;

    private final long bytesQuota;

    private final Instant sessionCutoff;

    private final Duration inactivityMax;

    private final UUID cookie;

    private final FbUser fbUser;

    private final AtomicReference<Instant> lastAccessed = new AtomicReference<>();

    private final AtomicLong bytesStreamed = new AtomicLong();

    private final ExpiringState<Pair<Album, Track>> randomTrack = new ExpiringState<>(Duration.ofHours(1));

    public Session(
        FbUser fbUser,
        Instant startTime,
        Instant sessionCutoff,
        Duration inactivityMax,
        AccessLevel accessLevel,
        long bytesQuota
    ) {

        this.fbUser = Objects.requireNonNull(fbUser, "fbUser");
        this.cookie = UUID.randomUUID();
        this.startTime = Objects.requireNonNull(startTime, "startTime");
        this.accessLevel = Objects.requireNonNull(accessLevel, "accessLevel");
        this.bytesQuota = bytesQuota;
        this.lastAccessed.set(this.startTime);
        this.sessionCutoff = Objects.requireNonNull(sessionCutoff, "cutoff");
        this.inactivityMax = Objects.requireNonNull(inactivityMax, "inactivityMax");
    }

    public Instant getStartTime() {

        return startTime.truncatedTo(ChronoUnit.SECONDS);
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

    public static boolean isPrivileged() {

        return false;
    }

    public void addBytesStreamed(long bytes) {

        bytesStreamed.addAndGet(bytes);
    }

    public void setLastAccessed(Req req) {

        lastAccessed.set(req.getTime());
    }

    public String getCurrentStatus(Instant time) {

        return getStatus(time).stream().distinct()
            .map(Status::getDescription)
            .collect(Collectors.joining(", "));
    }

    public Collection<Status> getStatus(Instant time) {

        return accessLevel.satisfies(AccessLevel.ADMIN) ? Collections.singleton(Status.OK) : Stream
            .of(
                sessionStatus(time),
                activityStatus(time),
                quotaStatus())
            .distinct()
            .collect(Collectors.toList());
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

        return sessionCutoff.truncatedTo(ChronoUnit.SECONDS);
    }

    public long getStreamedBytes() {

        return bytesStreamed.get();
    }

    public String getPrettyStreamedBytes() {

        return Print.bytes(bytesStreamed.get());
    }

    public String getPrettyStreamQuota() {

        return Print.bytes(bytesQuota);
    }

    public long getStreamQuota() {

        return bytesQuota;
    }

    public Optional<Pair<Album, Track>> setAccessibleTrack(Instant time, Pair<Album, Track> accessibleTrack) {

        if (!randomTrack.set(time, accessibleTrack)) {
            log.warn("Accessible track for {} is still {}, rejected: {}", this, randomTrack, accessibleTrack);
        } else {
            log.info("Accessible track for {} is now {}", this, accessibleTrack);
        }
        return randomTrack.get(time);
    }

    public Optional<Duration> getRandomTrackRemaining(Instant time) {

        return randomTrack.getRemaining(time);
    }

    public Optional<Pair<Album, Track>> getRandomTrack(Instant time) {

        return getRandomTrack(time, null);
    }

    public Optional<Pair<Album, Track>> getRandomTrack(Instant time, Supplier<Optional<Pair<Album, Track>>> newRandom) {

        return randomTrack.get(time, newRandom);
    }

    private Status sessionStatus(Instant time) {

        return time.isAfter(sessionCutoff)
            ? Status.SESSION_LENGTH_CUTOFF
            : Status.OK;
    }

    private Status activityStatus(Instant time) {

        if (bytesStreamed.get() == 0L) {
            return Status.OK;
        }
        Duration inactivity = Duration.between(lastAccessed.get(), time);
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
