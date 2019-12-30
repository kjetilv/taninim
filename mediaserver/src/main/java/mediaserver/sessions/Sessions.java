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

    public Session establish(WebPath webPath, FacebookUser user) {

        return sessions.compute(user, (__, session) -> {

            if (session != null && valid(webPath, session)) {
                log.info("Session active: {}", session);
                return session.accessedBy(webPath);
            }
            Session newSession = newSession(webPath, user);
            log.info("Session created: {} <= {}", newSession, session);
            return newSession.accessedBy(webPath);
        });
    }

    public Optional<Session> activeSession(WebPath webPath) {

        return session(webPath, false);
    }

    public Optional<Session> close(WebPath webPath) {

        return session(webPath, true)
            .map(Session::getFacebookUser)
            .map(sessions::remove);
    }

    private Optional<Session> session(WebPath webPath, boolean includeInvalid) {

        return webPath.getAuthentication()
            .flatMap(uuid ->
                singleSession(uuid)
                    .filter(session -> includeInvalid || valid(webPath, session)))
            .or(() ->
                devSession(webPath));
    }

    private Optional<Session> singleSession(UUID uuid) {

        Collection<Session> sessions = this.sessions.values().stream()
            .filter(withCookie(uuid))
            .collect(Collectors.toList());
        if (sessions.size() > 1) {
            throw new IllegalStateException("Multiple sessions for " + uuid);
        }
        return sessions.stream().findFirst();
    }

    private Session newSession(WebPath webPath, FacebookUser user) {

        Instant currentTime = webPath.getTime();
        return new Session(
            user,
            UUID.randomUUID(),
            currentTime,
            currentTime.plus(sessionLength),
            inactivityMax,
            bytesQuota);
    }

    private boolean valid(WebPath webPath, Session session) {

        Collection<Session.Status> statuses = sessionStatus(webPath, session);
        if (ok(statuses)) {
            return true;
        }
        log.info("Session disabled for {}: {} {}",
            webPath, session, statuses.stream().map(Enum::name).collect(Collectors.joining(", ")));
        return false;
    }

    private Collection<Session.Status> sessionStatus(WebPath webPath, Session session) {

        return status(
            session.sessionStatus(webPath),
            session.activityStatus(webPath),
            session.quotaStatus());
    }

    private Collection<Session.Status> status(Session.Status... stats) {

        return new HashSet<>(Arrays.asList(stats));
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
            Session session = newSession(webPath, DEV_USER);
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
