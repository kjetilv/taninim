package taninim.yellin;

import module java.base;
import com.github.kjetilv.uplift.hash.Hash;
import com.github.kjetilv.uplift.hash.HashKind;
import taninim.auth.Authed;
import taninim.music.medias.UserAuth;
import taninim.music.medias.UserAuths;
import taninim.music.medias.UserRequest;

import static java.util.Objects.requireNonNull;

final class DefaultAuthorizer implements Authorizer {

    static Authorizer create(
        Supplier<UserAuths> authIds,
        Consumer<UserAuths> updateAuthIds,
        Duration sessionDuration,
        Duration leaseDuration,
        Supplier<Instant> time
    ) {
        return new DefaultAuthorizer(authIds, updateAuthIds, sessionDuration, leaseDuration, time);
    }

    private final Supplier<UserAuths> authIds;

    private final Consumer<UserAuths> updateAuthIds;

    private final Duration sessionDuration;

    private final Duration leaseDuration;

    private final Supplier<Instant> time;

    private DefaultAuthorizer(
        Supplier<UserAuths> authIds,
        Consumer<UserAuths> updateAuthIds,
        Duration sessionDuration,
        Duration leaseDuration,
        Supplier<Instant> time
    ) {
        this.authIds = requireNonNull(authIds, "authIds");
        this.updateAuthIds = requireNonNull(updateAuthIds, "updateAuthIds");
        this.sessionDuration = requireNonNull(sessionDuration, "sessionDuration");
        this.leaseDuration = requireNonNull(leaseDuration, "ticketDuration");
        this.time = requireNonNull(time, "time");
    }

    @Override
    public Authed<UserAuth> login(String userId, boolean createSession) {
        var userAuths = this.authIds.get();
        var time = this.time.get();
        return userAuths.forUser(userId)
            .map(userAuth ->
                userAuth.withoutExpiredLeasesAt(time))
            .filter(userAuth ->
                userAuth.validAt(time))
            .map(Authed::authorized)
            .orElseGet(() ->
                createSession
                    ? Authed.authorized(createLogin(userAuths, userId, time))
                    : Authed.unauthorized("No current session"));
    }

    @Override
    public Authed<UserAuth> authorize(UserRequest request) {
        var userAuths = this.authIds.get();
        return conflictingLease(userAuths, request.userId(), request.albumId())
            .map(conflictingLease ->
                Authed.<UserAuth>unauthorized(remaining(conflictingLease)))
            .orElseGet(() -> {
                var time = this.time.get();
                var requested = requestedAuth(request, time);
                var auths = userAuths.updatedWith(requested, time);
                updateAuthIds.accept(auths);
                var userAuth = auths.requestedAuth(request);
                return Authed.resolve(userAuth);
            });
    }

    @Override
    public Authed<UserAuth> deauthorize(UserRequest request) {
        var userAuths = this.authIds.get();
        UserAuths auths = userAuths.without(requestedAuth(request, time.get()));
        updateAuthIds.accept(auths);
        return Authed.resolve(auths.requestedAuth(request));
    }

    private String remaining(UserAuth.AlbumLease conflictingLease) {
        return "Conflicting lease for " + Duration.between(time.get(), conflictingLease.expiry());
    }

    private UserAuth createLogin(UserAuths userAuths, String userId, Instant time) {
        var userAuth = new UserAuth(userId, time.plus(sessionDuration));
        updateAuthIds.accept(userAuths.updatedWith(userAuth, time));
        return userAuth;
    }

    private UserAuth requestedAuth(UserRequest request, Instant time) {
        return new UserAuth(
            request.userId(),
            time,
            request.token(),
            List.of(
                new UserAuth.AlbumLease(
                    request.albumId(),
                    time.plus(leaseDuration)
                ))
        );
    }

    private static Optional<UserAuth.AlbumLease> conflictingLease(
        UserAuths userAuths,
        String userId,
        Hash<HashKind.K128> uuid
    ) {
        return userAuths.auths()
            .stream()
            .filter(notFor(userId))
            .flatMap(existingAuth ->
                conflictingLeases(existingAuth, uuid))
            .findFirst();
    }

    private static Predicate<UserAuth> notFor(String userId) {
        return userAuth ->
            !userAuth.matches(userId);
    }

    private static Stream<UserAuth.AlbumLease> conflictingLeases(UserAuth existingAuth, Hash<HashKind.K128> uuid) {
        return existingAuth.albumLeases()
            .stream()
            .filter(lease ->
                lease.isFor(uuid));
    }
}
