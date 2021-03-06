package mediaserver.sessions;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import mediaserver.externals.FbUser;
import mediaserver.http.Req;
import mediaserver.util.MostlyOnce;
import mediaserver.util.Print;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Sessions {

    private static final Logger log = LoggerFactory.getLogger(Sessions.class);

    private final Map<FbUser, Collection<Session>> sessionsMap = new ConcurrentHashMap<>();

    private final Supplier<Ids> ids;

    private final Duration sessionLength;

    private final Duration inactivityMax;

    private final long bytesQuota;

    private final boolean devLogin;

    private final Function<Req, Session> devSession = MostlyOnce.compute(req -> {
        Session session = newSession(req, DEV_USER, AccessLevel.ADMIN);
        log.warn("Established dev session: {}", session);
        return session;
    });

    public Sessions(
        Supplier<Ids> ids,
        Duration sessionLength,
        Duration inactivityMax,
        long bytesQuota,
        boolean devLogin
    ) {

        this.ids = ids;
        this.sessionLength = sessionLength;
        this.inactivityMax = inactivityMax;
        this.bytesQuota = bytesQuota;
        this.devLogin = devLogin;
        if (this.devLogin) {
            log.warn("{} will allow login as dev", this);
        }
    }

    public Session create(Req req, FbUser user) {

        AccessLevel accessLevel = ids.get().resolve(user);
        if (accessLevel.satisfies(AccessLevel.LOGIN)) {
            Session newSession = newSession(req, user, accessLevel);
            ejectedSessions(user, newSession).forEach(ejectedSession ->
                log.info("Ejected {} session for: {}",
                    valid(req, ejectedSession) ? "live" : "expired", ejectedSession));
            log.info("Session created for {}: {}", user, newSession);
            return newSession;
        }
        throw new IllegalArgumentException("Unauthorized: " + user);
    }

    public Function<Req, Req> binder() {

        return req ->
            getExistingSession(req, AccessLevel.LOGIN, false)
                .map(req::boundTo)
                .orElse(req);
    }

    public Optional<Session> close(UUID sessionId) {

        Optional<Map.Entry<FbUser, Collection<Session>>> entry = sessionsMap.entrySet().stream()
            .filter(e ->
                e.getValue().stream().map(Session::getCookie).anyMatch(sessionId::equals))
            .findFirst();
        Optional<Session> session = entry.flatMap(e ->
            e.getValue().stream()
                .filter(s ->
                    s.getCookie().equals(sessionId))
                .findFirst());
        session.ifPresent(s ->
            entry.ifPresent(e ->
                e.getValue().remove(s)));
        entry.filter(e -> e.getValue().isEmpty())
            .ifPresent(e -> sessionsMap.remove(e.getKey()));

        return session;
    }

    public Optional<Session> close(Req req) {

        return getExistingSession(req, AccessLevel.NONE, true).map(session -> {
            FbUser fbUser = session.getFbUser();
            Collection<Session> sessions = sessionsMap.get(fbUser);
            if (sessions == null) {
                log.info("No sessions to close for {}", fbUser);
                return null;
            }
            if (sessions.isEmpty()) {
                log.info("No sessions to close for {}", fbUser);
                sessionsMap.remove(fbUser);
                return null;
            }
            if (sessions.remove(session)) {
                if (sessions.isEmpty()) {
                    sessionsMap.remove(fbUser);
                }
                log.info("{} removed session {}, {} left: {}/{}",
                    this, session, sessions.size(),
                    this.sessionsMap.size(), this.sessionsMap.values().stream().flatMap(Collection::stream).count());
            } else {
                log.warn("{} found no removable session: {}", this, session);
            }
            return session;
        });
    }

    public Collection<Session> list() {

        return this.sessionsMap.values().stream()
            .flatMap(Collection::stream)
            .sorted(Comparator.comparing(Session::getStartTime))
            .collect(Collectors.toList());
    }

    private Stream<Session> ejectedSessions(FbUser fbUser, Session session) {

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

    private Optional<Session> getExistingSession(Req req, AccessLevel accessLevel, boolean includeInvalid) {

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

        if (accessLevel.satisfies(AccessLevel.LOGIN)) {
            return new Session(
                fbUser,
                req.getTime(),
                req.getTime().plus(sessionLength),
                inactivityMax,
                accessLevel,
                req.isLocal(),
                bytesQuota);
        }
        throw new IllegalArgumentException("Invalid access level: " + accessLevel);
    }

    private Optional<Session> devSession(Req req) {

        return devLogin ? devSession.andThen(Optional::ofNullable).apply(req) : Optional.empty();
    }

    private static final FbUser DEV_USER = new FbUser("dev", "dev");

    private static boolean valid(Req req, Session session) {

        if (session.isValid(req.getTime())) {
            return true;
        }
        log.info("Session disabled for {}: {} {}",
            req, session, session.getStatus(req.getTime()).stream().map(Enum::name)
                .collect(Collectors.joining(", ")));
        return false;
    }

    private static Predicate<Session> withCookie(UUID uuid) {

        return session ->
            Objects.equals(session.getCookie(), uuid);
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
