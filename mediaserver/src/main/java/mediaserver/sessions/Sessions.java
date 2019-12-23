package mediaserver.sessions;

import io.netty.handler.codec.http.HttpRequest;
import mediaserver.externals.FacebookUser;
import mediaserver.http.NettyHandler;
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

public final class Sessions {

    private static final Logger log = LoggerFactory.getLogger(Sessions.class);

    private final Map<FacebookUser, Session> sessions = new ConcurrentHashMap<>();

    private final Duration sessionLength;

    private final Duration inactivityMax;

    private final Clock clock;

    private final boolean dev;

    public Sessions(Duration sessionLength, Duration inactivityMax, Clock clock, boolean dev) {

        this.sessionLength = sessionLength;
        this.inactivityMax = inactivityMax;
        this.clock = clock;
        this.dev = dev;
        if (this.dev) {
            log.warn("{} started in dev mode", this);
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

    public Optional<FacebookUser> activeUser(HttpRequest req) {

        return NettyHandler.authenticationId(req)
            .flatMap(this::activeUser)
            .or(this::devUser);
    }

    public Optional<Session> close(HttpRequest req) {

        return activeUser(req).map(sessions::remove);
    }

    public Optional<FacebookUser> activeUser(UUID uuid) {

        return sessions.values().stream()
            .filter(session ->
                Objects.equals(uuid, session.getCookie()))
            .findFirst()
            .map(Session::getFacebookUser)
            .or(this::devUser);
    }

    private Optional<FacebookUser> devUser() {

        if (dev) {
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
