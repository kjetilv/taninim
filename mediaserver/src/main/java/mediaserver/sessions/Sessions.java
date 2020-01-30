package mediaserver.sessions;

import mediaserver.externals.FbUser;
import mediaserver.http.Req;
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
import java.util.stream.Stream;

public final class Sessions {

    private static final Logger log = LoggerFactory.getLogger(Sessions.class);

    private final Map<FbUser, Collection<Session>> sessionsMap = new ConcurrentHashMap<>();

    private final Duration sessionLength;

    private final Duration inactivityMax;

    private final long bytesQuota;

    private final boolean devLogin;

    private final Function<Req, Session> devSession = MostlyOnce.compute(req -> {
        Session session = newSession(req, DEV_USER, AccessLevel.STREAM_CURATED);
        log.warn("Established dev session: {}", session);
        return session;
    });

    private static final FbUser DEV_USER = new FbUser("dev", "dev");

    public Sessions(Duration sessionLength, Duration inactivityMax, long bytesQuota, boolean devLogin) {

        this.sessionLength = sessionLength;
        this.inactivityMax = inactivityMax;
        this.bytesQuota = bytesQuota;
        this.devLogin = devLogin;
        if (this.devLogin) {
            log.warn("{} will allow login as dev", this);
        }
    }

    public UUID newSessionUUID(Req req, FbUser fbUser, AccessLevel accessLevel) {

        Session newSession = newSession(req, fbUser, accessLevel);
        ejectedSessions(fbUser, newSession).forEach(ejectedSession -> {
            log.info("Ejected {} session for: {}",
                valid(req, ejectedSession) ? "live" : "expired", ejectedSession);
        });
        log.info("Session created for {}: {}", fbUser, newSession);
        return newSession.getCookie();
    }

    public Stream<Session> ejectedSessions(FbUser fbUser, Session session) {

        if (session.hasLevel(AccessLevel.ADMIN)) {
            sessionsMap.computeIfAbsent(fbUser, __ ->
                new CopyOnWriteArrayList<>()
            ).add(session);
            return Stream.empty();
        }
        Collection<Session> existingSessions =
            sessionsMap.put(fbUser, new CopyOnWriteArrayList<>(Collections.singleton(session)));
        return Optional.ofNullable(existingSessions)
            .stream()
            .flatMap(Collection::stream);
    }

    public Req bind(Req req) {

        return session(req, AccessLevel.LOGIN, false)
            .map(req::bind)
            .orElse(req);
    }

    public Optional<Session> close(Req req) {

        return session(req, AccessLevel.NONE, true)
            .map(session -> {
                FbUser fbUser = session.getFbUser();
                Optional.of(sessionsMap.get(fbUser))
                    .filter(sessions ->
                        !sessions.isEmpty())
                    .ifPresentOrElse(
                        sessions -> {
                            if (sessions.remove(session)) {
                                if (sessions.isEmpty()) {
                                    sessionsMap.remove(fbUser);
                                }
                                log.info("{} removed session, {} left: {}/{}",
                                    this, sessions.size(), this.sessionsMap.size(), session);
                            } else {
                                log.warn("{} found no removable session: {}",
                                    this, session);
                            }
                        },
                        () ->
                            log.info("No sessions to close for {}", fbUser));
                return session;
            });
    }

    public Collection<Session> list() {

        return this.sessionsMap.values().stream()
            .flatMap(Collection::stream)
            .sorted(Comparator.comparing(Session::getStartTime))
            .collect(Collectors.toList());
    }

    private Optional<Session> session(Req req, AccessLevel accessLevel, boolean includeInvalid) {

        return req.getAuthentication()
            .flatMap(uuid ->
                uniqueSession(uuid).filter(session ->
                    includeInvalid || valid(req, session) && session.hasLevel(accessLevel)))
            .or(() ->
                devSession(req));
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

    private Session newSession(Req req, FbUser fbUser, AccessLevel accessLevel) {

        return new Session(
            fbUser,
            UUID.randomUUID(),
            req.getTime(),
            req.getTime().plus(sessionLength),
            inactivityMax,
            accessLevel,
            bytesQuota
        ).accessedBy(req);
    }

    private boolean valid(Req req, Session session) {

        if (session.isValid(req.getTime())) {
            return true;
        }
        log.info("Session disabled for {}: {} {}",
            req, session, session.getStatus(req.getTime()).stream().map(Enum::name)
                .collect(Collectors.joining(", ")));
        return false;
    }

    private Predicate<Session> withCookie(UUID uuid) {

        return session ->
            Objects.equals(session.getCookie(), uuid);
    }

    private Optional<Session> devSession(Req req) {

        return devLogin ? devSession.andThen(Optional::ofNullable).apply(req) : Optional.empty();
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
