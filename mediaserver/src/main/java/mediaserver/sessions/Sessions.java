package mediaserver.sessions;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import mediaserver.externals.FacebookUser;
import mediaserver.gui.GUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static io.netty.handler.codec.http.HttpHeaderNames.COOKIE;

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

        return cookies(req)
            .filter(cookie ->
                cookie.name().equalsIgnoreCase(GUI.ID_COOKIE))
            .map(cookie ->
                UUID.fromString(cookie.value()))
            .findFirst()
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

    private Stream<Cookie> cookies(HttpRequest req) {

        return Optional.of(req.headers())
            .map(headers -> headers.get(COOKIE))
            .map(ServerCookieDecoder.STRICT::decode)
            .stream()
            .flatMap(Collection::stream);
    }

    public Optional<Session> close(HttpRequest req) {

        return activeUser(req).map(sessions::remove);
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
