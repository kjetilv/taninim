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
import java.util.stream.Stream;

public final class Sessions {

    private static final Logger log = LoggerFactory.getLogger(Sessions.class);

    private final Map<FacebookUser, Session> sessions = new ConcurrentHashMap<>();

    private final Duration sessionLength;

    private final Duration inactivityMax;

    private final long bytesQuota;

    private final boolean devLogin;

    private static final FacebookUser DEV_USER = new FacebookUser("dev", "dev");

    private static final Collection<Session.Status> OK_STATUS = Collections.singleton(Session.Status.OK);

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
                return session.accessedAt(webPath.getTime());
            }
            Session newSession = newSessionAt(webPath.getTime(), user);
            log.info("Session created: {} <= {}", newSession, session);
            return newSession;
        });
    }

    public Optional<Session> activeSession(WebPath webPath) {

        return webPath.getAuthentication()
            .flatMap(uuid -> {
                Collection<Session> sessions = this.sessions.values().stream()
                    .filter(withCookie(uuid))
                    .collect(Collectors.toList());
                if (sessions.size() > 1) {
                    throw new IllegalStateException("Multiple sessions for " + uuid);
                }

                return sessions.stream()
                    .findFirst()
                    .filter(session ->
                        valid(webPath, session));
            })
            .or(() ->
                devSession(webPath));
    }

    public Optional<Session> close(WebPath webPath) {

        return activeSession(webPath).map(Session::getFacebookUser).map(sessions::remove);
    }

    private boolean valid(WebPath webPath, Session session) {

        Collection<Session.Status> statuses = Stream.of(
            session.stillActive(webPath.getTime()),
            session.withinQuota()
        ).collect(Collectors.toUnmodifiableSet());
        if (statuses.equals(OK_STATUS)) {
            return true;
        }
        log.info("Session disabled for {}: {} {}", webPath, session, statuses.stream().map(Enum::name).collect(
            Collectors.joining(", ")));
        return false;
    }

    private Session newSessionAt(Instant currentTime, FacebookUser facebookUser) {

        UUID cookie = UUID.randomUUID();
        return new Session(
            facebookUser,
            cookie,
            currentTime,
            currentTime.plus(sessionLength),
            inactivityMax,
            bytesQuota);
    }

    private Predicate<Session> withCookie(UUID uuid) {

        return session ->
            Objects.equals(session.getCookie(), uuid);
    }

    private Optional<Session> devSession(WebPath webPath) {

        if (devLogin) {
            Session session = newSessionAt(webPath.getTime(), DEV_USER);
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
