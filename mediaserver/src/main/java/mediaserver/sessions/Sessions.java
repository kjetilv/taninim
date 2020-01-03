package mediaserver.sessions;

import mediaserver.externals.FacebookUser;
import mediaserver.http.WebPath;
import mediaserver.util.Print;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class Sessions {

    private static final Logger log = LoggerFactory.getLogger(Sessions.class);

    private final Map<FacebookUser, Session> sessions = new ConcurrentHashMap<>();

    private final Duration sessionLength;

    private final Duration inactivityMax;

    private final long bytesQuota;

    private final boolean devLogin;

    private static final FacebookUser DEV_USER = new FacebookUser("dev", "dev");

    public Sessions(Duration sessionLength, Duration inactivityMax, long bytesQuota, boolean devLogin) {

        this.sessionLength = sessionLength;
        this.inactivityMax = inactivityMax;
        this.bytesQuota = bytesQuota;
        this.devLogin = devLogin;
        if (this.devLogin) {
            log.warn("{} will allow login as dev", this);
        }
    }

    public UUID newSessionUUID(WebPath webPath, FacebookUser facebookUser, AccessLevel accessLevel) {
        Session session =
            newSession(webPath, facebookUser, accessLevel).accessedBy(webPath);
        Session previousSession =
            sessions.put(facebookUser, session);

        if (previousSession == null) {
            log.info("Session created for {}: {}", facebookUser, session);
        } else {
            boolean wasValid = valid(webPath, previousSession);
            log.info("Session created for {}: {}, replacing {} previous: {}",
                facebookUser, session, wasValid ? "still valid" : "invalidated", previousSession);
        }
        return session.getCookie();
    }

    public WebPath instrument(WebPath webPath) {

        return session(webPath, false, AccessLevel.LOGIN)
            .map(webPath::with)
            .orElse(webPath);
    }

    public Optional<Session> close(WebPath webPath) {

        return session(webPath, true, AccessLevel.NONE)
            .map(Session::getFacebookUser)
            .map(sessions::remove)
            .stream()
            .peek(session ->
                log.info("{} removed session, {} left: {}", this, sessions.size(), session))
            .findFirst();
    }

    public Collection<Session> list() {

        return this.sessions.values().stream()
            .sorted(Comparator.comparing(Session::getStartTime))
            .collect(Collectors.toList());
    }

    private Optional<Session> session(WebPath webPath, boolean includeInvalid, AccessLevel accessLevel) {

        return webPath.getAuthentication()
            .flatMap(uuid ->
                uniqueSession(uuid).filter(session ->
                    includeInvalid || valid(webPath, session) && session.hasLevel(accessLevel)))
            .or(() ->
                devSession(webPath));
    }

    private Optional<Session> uniqueSession(UUID uuid) {

        Collection<Session> sessions = this.sessions.values().stream()
            .filter(withCookie(uuid))
            .collect(Collectors.toList());
        if (sessions.size() > 1) {
            throw new IllegalStateException("Multiple sessions for " + uuid + ": " + sessions);
        }
        return sessions.stream().findFirst();
    }

    private Session newSession(WebPath webPath, FacebookUser facebookUser, AccessLevel accessLevel) {

        Instant currentTime = webPath.getTime();
        return new Session(
            facebookUser,
            UUID.randomUUID(),
            currentTime,
            currentTime.plus(sessionLength),
            inactivityMax,
            accessLevel,
            bytesQuota);
    }

    private boolean valid(WebPath webPath, Session session) {

        Collection<Session.Status> statuses = session.status(webPath);
        boolean ok = ok(statuses);
        if (ok) {
            return true;
        }
        String statusString = statuses.stream().map(Enum::name)
            .collect(Collectors.joining(", "));
        log.info("Session disabled for {}: {} {}",
            webPath, session, statusString);
        return false;
    }

    private boolean ok(Collection<Session.Status> statuses) {

        return statuses.size() == 1 && statuses.iterator().next() == Session.Status.OK;
    }

    private Predicate<Session> withCookie(UUID uuid) {

        return session ->
            Objects.equals(session.getCookie(), uuid);
    }

    private Optional<Session> devSession(WebPath webPath) {

        if (devLogin) {
            Session session = newSession(webPath, DEV_USER, AccessLevel.ADMIN);
            log.warn("Established dev session: {}", session);
            return Optional.of(session);
        }
        return Optional.empty();
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + sessions.keySet() +
            " bytesQuota:" + Print.bytes(bytesQuota) +
            " sessionLength:" + sessionLength +
            " inactivityMax:" + inactivityMax +
            " devLogin:" + devLogin +
            "]";
    }
}
