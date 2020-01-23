package mediaserver.sessions;

import mediaserver.externals.FacebookUser;
import mediaserver.http.WebPath;
import mediaserver.util.MostlyOnce;
import mediaserver.util.Print;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class Sessions {

    private static final Logger log = LoggerFactory.getLogger(Sessions.class);

    private final Map<FacebookUser, Collection<Session>> sessionsMap = new ConcurrentHashMap<>();

    private final Duration sessionLength;

    private final Duration inactivityMax;

    private final long bytesQuota;

    private final boolean devLogin;

    private final Function<WebPath, Session> devSession = MostlyOnce.compute(webPath -> {
        Session session = newSession(webPath, DEV_USER, AccessLevel.ADMIN);
        log.warn("Established dev session: {}", session);
        return session;
    });

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

        Session newSession = newSession(webPath, facebookUser, accessLevel);
        ejectedSession(facebookUser, newSession).ifPresentOrElse(
            previous -> {
                boolean wasValid = valid(webPath, previous);
                log.info("Session created for {}: {}, previous was {}: {}",
                    facebookUser, newSession, wasValid ? "live" : "expired",
                    previous);
            },
            () ->
                log.info("Session created for {}: {}", facebookUser, newSession));
        return newSession.getCookie();
    }

    public Optional<Session> ejectedSession(FacebookUser facebookUser, Session session) {

        if (session.hasLevel(AccessLevel.ADMIN)) {
            sessionsMap.computeIfAbsent(facebookUser, __ ->
                new CopyOnWriteArrayList<>()
            ).add(session);
            return Optional.empty();
        }
        return Optional.ofNullable(sessionsMap.put(facebookUser, Collections.singleton(session))).stream()
            .flatMap(Collection::stream)
            .findFirst();
    }

    public WebPath bind(WebPath webPath) {

        return session(webPath, AccessLevel.LOGIN, false)
            .map(webPath::bind)
            .orElse(webPath);
    }

    public Optional<Session> close(WebPath webPath) {

        return session(webPath, AccessLevel.NONE, true)
            .map(session -> {
                FacebookUser facebookUser = session.getFacebookUser();
                Optional.of(sessionsMap.get(facebookUser))
                    .filter(sessions ->
                        !sessions.isEmpty())
                    .ifPresentOrElse(
                        sessions -> {
                            if (sessions.remove(session)) {
                                if (sessions.isEmpty()) {
                                    sessionsMap.remove(facebookUser);
                                }
                                log.info("{} removed session, {} left: {}/{}",
                                    this, sessions.size(), this.sessionsMap.size(), session);
                            } else {
                                log.warn("{} found no removable session: {}",
                                    this, session);
                            }
                        },
                        () ->
                            log.info("No sessions to close for {}", facebookUser));
                return session;
            });
    }

    public Collection<Session> list() {

        return this.sessionsMap.values().stream()
            .flatMap(Collection::stream)
            .sorted(Comparator.comparing(Session::getStartTime))
            .collect(Collectors.toList());
    }

    private Optional<Session> session(WebPath webPath, AccessLevel accessLevel, boolean includeInvalid) {

        return webPath.getAuthentication()
            .flatMap(uuid ->
                uniqueSession(uuid).filter(session ->
                    includeInvalid || valid(webPath, session) && session.hasLevel(accessLevel)))
            .or(() ->
                devSession(webPath));
    }

    private Optional<Session> uniqueSession(UUID uuid) {

        Collection<Session> sessions = this.sessionsMap.values().stream()
            .flatMap(Collection::stream)
            .filter(withCookie(uuid))
            .collect(Collectors.toList());
        if (sessions.size() > 1) {
            throw new IllegalStateException("Multiple sessions for " + uuid + ": " + sessions);
        }
        return sessions.stream().findFirst();
    }

    private Session newSession(WebPath webPath, FacebookUser facebookUser, AccessLevel accessLevel) {

        return new Session(
            facebookUser,
            UUID.randomUUID(),
            webPath.getTime(),
            webPath.getTime().plus(sessionLength),
            inactivityMax,
            accessLevel,
            bytesQuota
        ).accessedBy(webPath);
    }

    private boolean valid(WebPath webPath, Session session) {

        if (session.isValid(webPath.getTime())) {
            return true;
        }
        log.info("Session disabled for {}: {} {}",
            webPath, session, session.getStatus(webPath.getTime()).stream().map(Enum::name)
                .collect(Collectors.joining(", ")));
        return false;
    }

    private Predicate<Session> withCookie(UUID uuid) {

        return session ->
            Objects.equals(session.getCookie(), uuid);
    }

    private Optional<Session> devSession(WebPath webPath) {

        return devLogin ? devSession.andThen(Optional::ofNullable).apply(webPath) : Optional.empty();
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + sessionsMap.keySet() +
            " bytesQuota:" + Print.bytes(bytesQuota) +
            " sessionLength:" + sessionLength +
            " inactivityMax:" + inactivityMax +
            " devLogin:" + devLogin +
            "]";
    }
}
