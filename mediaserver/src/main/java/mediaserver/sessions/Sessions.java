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

    public Session establish(WebPath webPath, FacebookUser user, AccessLevel level) {
        Session session = newSession(webPath, user, level).accessedBy(webPath);
        Session previousSession = sessions.put(user, session);
        if (previousSession == null) {
            log.info("Session created: {}", session);
        } else {
            boolean wasValid = valid(webPath, previousSession);
            log.info("Session created, replacing {}: {} / {}",
                session, wasValid ? "live existing" : "invalid", previousSession);
        }
        return session;
    }

    public Optional<Session> activeSession(WebPath webPath, AccessLevel accessLevel) {

        return session(webPath, false, accessLevel);
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

    private Session newSession(WebPath webPath, FacebookUser user, AccessLevel accessLevel) {

        Instant currentTime = webPath.getTime();
        return new Session(
            user,
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
            Session session = newSession(webPath, DEV_USER, AccessLevel.STREAM);
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
