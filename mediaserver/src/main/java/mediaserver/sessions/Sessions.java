package mediaserver.sessions;

import io.netty.handler.codec.http.HttpRequest;
import mediaserver.externals.FacebookUser;
import mediaserver.http.NettyHandler;
import mediaserver.util.MostlyOnce;
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
import java.util.function.Supplier;

public final class Sessions {

    private static final Logger log = LoggerFactory.getLogger(Sessions.class);

    private final Map<FacebookUser, Session> sessions = new ConcurrentHashMap<>();

    private final Supplier<Ids> ids;

    private final Duration sessionLength;

    private final Clock clock;

    private final boolean dev;

    public Sessions(Supplier<Ids> ids, Duration sessionLength, Clock clock, boolean dev) {

        this.ids = ids;
        this.sessionLength = sessionLength;
        this.clock = clock;
        this.dev = dev;
        if (this.dev) {
            log.warn("{} started in dev mode", this);
        }
    }

    public Session establish(FacebookUser user) {

        return sessions.compute(user, this::resolveSession);
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

    private Session resolveSession(FacebookUser facebookUser, Session oldSession) {

        Supplier<Instant> currentTime = MostlyOnce.get(this::now);
        if (oldSession == null || oldSession.timedout(currentTime.get())) {
            Session session = new Session(facebookUser, UUID.randomUUID(), cutoff(currentTime.get()));
            log.info("{}: New session {}, previous: {}", facebookUser, session, oldSession);
            return session;
        }
        log.info("{} reused session: {}", facebookUser, oldSession);
        return oldSession;
    }

    private Instant now() {

        return clock.instant();
    }

    private Instant cutoff(Instant time) {

        return time.plus(sessionLength);
    }
}
