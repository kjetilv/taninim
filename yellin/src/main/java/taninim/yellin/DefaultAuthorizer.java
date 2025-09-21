package taninim.yellin;

import module java.base;
import module taninim.taninim;

import static java.util.Objects.requireNonNull;

final class DefaultAuthorizer implements Authorizer {

    private final Supplier<UserAuths> authIds;

    private final Consumer<UserAuths> updateAuthIds;

    private final Duration sessionDuration;

    private final Duration leaseDuration;

    private final Supplier<Instant> time;

    DefaultAuthorizer(
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
    public Optional<UserAuth> login(String userId, boolean createSession) {
        UserAuths userAuths = this.authIds.get();
        Instant time = this.time.get();
        return userAuths.forUser(userId)
            .map(userAuth ->
                userAuth.withoutExpiredLeasesAt(time))
            .filter(userAuth ->
                userAuth.validAt(time))
            .or(() ->
                createSession
                    ? Optional.of(createLogin(userAuths, userId, time))
                    : Optional.empty());
    }

    @Override
    public Optional<UserAuth> authorize(UserRequest request) {
        UserAuths userAuths = this.authIds.get();
        return conflictingLease(request, userAuths)
            .map(_ ->
                Optional.<UserAuth>empty())
            .orElseGet(() ->
                addAuth(userAuths, request));
    }

    @Override
    public Optional<UserAuth> deauthorize(UserRequest request) {
        UserAuths userAuths = this.authIds.get();
        return updatedUserAuths(
            request,
            userAuths.without(requestedAuth(request, time.get()))
        );
    }

    private Optional<UserAuth> addAuth(UserAuths userAuths, UserRequest request) {
        Instant time = this.time.get();
        UserAuth requestedAuth = requestedAuth(request, time);
        UserAuths updatedAuths = userAuths.updatedWith(requestedAuth, time);
        return updatedUserAuths(request, updatedAuths);
    }

    private UserAuth createLogin(UserAuths userAuths, String userId, Instant time) {
        UserAuth userAuth = new UserAuth(userId, time.plus(sessionDuration));
        updateAuthIds.accept(userAuths.updatedWith(userAuth, time));
        return userAuth;
    }

    private Optional<UserAuth> updatedUserAuths(UserRequest request, UserAuths auths) {
        updateAuthIds.accept(auths);
        return auths.forAuth(request);
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
        UserRequest request,
        UserAuths userAuths
    ) {
        return userAuths.userAuths()
            .stream()
            .filter(userAuth ->
                isForOther(userAuth, request))
            .flatMap(existingAuth ->
                existingAuth.albumLeases()
                    .stream()
                    .filter(lease ->
                        lease.isFor(request.albumId())))
            .findFirst();
    }

    private static boolean isForOther(UserAuth userAuth, UserRequest request) {
        return !userAuth.matches(request.userId());
    }
}
