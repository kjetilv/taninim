package mediaserver.sessions;

import io.netty.handler.codec.http.HttpRequest;
import mediaserver.externals.FacebookUser;
import mediaserver.http.Nettish;
import mediaserver.util.MostlyOnce;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class Sessions {

    private static final Logger log = LoggerFactory.getLogger(Sessions.class);

    private final Map<FacebookUser, Session> sessions = new ConcurrentHashMap<>();

    private final Supplier<Ids> ids;

    private final Duration sessionLength;

    private final Clock clock;

    public Sessions(Supplier<Ids> ids, Duration sessionLength, Clock clock) {

        this.ids = ids;
        this.sessionLength = sessionLength;
        this.clock = clock;
    }

    public Session sessionUp(FacebookUser user) {

        return sessions.compute(user, this::resolveSession);
    }

    public Optional<Session> closeSession(HttpRequest req) {

        return activeUser(req).map(sessions::remove);
    }

    public Optional<FacebookUser> activeUser(HttpRequest req) {

        return activeSession(req)
            .map(Session::getFacebookUser)
            .or(() ->
                ids.get().dev().map(FacebookUser::new));
    }

    public Optional<FacebookUser> activeUser(UUID uuid) {

        return session(uuid)
            .map(Session::getFacebookUser)
            .or(() ->
                ids.get().dev().map(dev -> new FacebookUser(dev, dev)));
    }

    private Optional<Session> session(UUID id) {

        return sessions.values().stream()
            .filter(session ->
                session.getCookie().equals(id))
            .findFirst();
    }

    private Optional<Session> activeSession(HttpRequest req) {

        return Nettish.authCookie(req).flatMap(this::session);
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
