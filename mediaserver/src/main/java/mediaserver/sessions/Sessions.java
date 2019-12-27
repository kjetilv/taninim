package mediaserver.sessions;

import mediaserver.externals.FacebookUser;
import mediaserver.http.WebPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class Sessions {

    private static final Logger log = LoggerFactory.getLogger(Sessions.class);

    private final Map<FacebookUser, Session> sessions = new ConcurrentHashMap<>();

    private final Duration sessionLength;

    private final Duration inactivityMax;

    private final Clock clock;

    private final boolean devLogin;

    private static final FacebookUser DEV_USER = new FacebookUser("dev", "dev");

    public Sessions(Duration sessionLength, Duration inactivityMax, Clock clock, boolean devLogin) {

        this.sessionLength = sessionLength;
        this.inactivityMax = inactivityMax;
        this.clock = clock;
        this.devLogin = devLogin;
        if (this.devLogin) {
            log.warn("{} will allow login as dev", this);
        }
    }

    public Session establish(FacebookUser user) {

        return sessions.compute(user, (__, oldSession) -> {

            Instant time = this.now();
            if (activeAt(time, oldSession)) {
                log.info("Active: {}", oldSession);
                return oldSession.accessedAt(time);
            }
            Session newSession = newSessionAt(time, user);
            log.info("Created: {} <= {}", newSession, oldSession);
            return newSession;
        });
    }

    public Optional<Session> activeSession(WebPath webPath) {

        return webPath.getAuthentication()
            .flatMap(uuid ->
                sessions.values().stream()
                    .filter(withCookie(uuid))
                    .findFirst()
                    .map(this::accessed))
            .or(this::devSession);
    }

    public Optional<Session> close(WebPath webPath) {

        return activeSession(webPath).map(Session::getFacebookUser).map(sessions::remove);
    }

    private Session accessed(Session session) {

        return session.accessedAt(now());
    }

    private boolean activeAt(Instant currentTime, Session oldSession) {

        return oldSession != null && !oldSession.expiredAt(currentTime);
    }

    private Session newSessionAt(Instant currentTime, FacebookUser facebookUser) {

        UUID cookie = UUID.randomUUID();
        return new Session(
            facebookUser,
            cookie,
            currentTime,
            currentTime.plus(sessionLength),
            inactivityMax);
    }

    private Predicate<Session> withCookie(UUID uuid) {

        return session ->
            Objects.equals(session.getCookie(), uuid);
    }

    private Optional<Session> devSession() {

        if (devLogin) {
            Session session = newSessionAt(clock.instant(), DEV_USER);
            log.warn("Established dev session: {}", session);
            return Optional.of(session);
        }
        return Optional.empty();
    }

    private Instant now() {

        return clock.instant();
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + sessions.keySet() + "]";
    }
}
