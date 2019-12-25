package mediaserver.sessions;

import mediaserver.externals.FacebookUser;
import mediaserver.http.WebPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class Sessions {

    private static final Logger log = LoggerFactory.getLogger(Sessions.class);

    private final Map<FacebookUser, Session> sessions = new ConcurrentHashMap<>();

    private final Duration sessionLength;

    private final Duration inactivityMax;

    private final Clock clock;

    private final boolean devLogin;

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

        return sessions.compute(user, (facebookUser, oldSession) -> {

            Instant currentTime = this.now();
            if (oldSession == null || oldSession.expiredAt(currentTime)) {
                Session session = new Session(
                    facebookUser, UUID.randomUUID(), currentTime, cutoff(currentTime), inactivityMax);
                log.info("{}: New session {}, previous: {}", facebookUser, session, oldSession);
                return session;
            }
            log.info("{} reused session: {}", facebookUser, oldSession);
            return oldSession.accessedAt(currentTime);
        });
    }

    public Optional<FacebookUser> activeUser(WebPath webPath) {

        return webPath.getAuthentication()
            .flatMap(this::userFor)
            .or(this::devUser);
    }

    private Optional<FacebookUser> userFor(UUID uuid) {

        return sessions.values().stream()
            .filter(session ->
                Objects.equals(uuid, session.getCookie()))
            .findFirst()
            .map(Session::getFacebookUser);
    }

    public Optional<Session> close(WebPath webPath) {

        return activeUser(webPath).map(sessions::remove);
    }

    private Optional<FacebookUser> devUser() {

        if (devLogin) {
            FacebookUser devUser = new FacebookUser("dev", "dev");
            log.warn("Returning dev user: {}", devUser);
            return Optional.of(devUser);
        }
        return Optional.empty();
    }

    private Instant now() {

        return clock.instant();
    }

    private Instant cutoff(Instant time) {

        return time.plus(sessionLength);
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + sessions.keySet() + "]";
    }
}
