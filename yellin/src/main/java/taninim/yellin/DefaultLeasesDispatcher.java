package taninim.yellin;

import module java.base;
import module taninim.fb;
import module taninim.taninim;
import module uplift.uuid;

import static java.util.Objects.requireNonNull;

public class DefaultLeasesDispatcher implements LeasesDispatcher {

    private final FbAuthenticator fbAuthenticator;

    private final Authorizer authorizer;

    private final LeasesRegistry leasesRegistry;

    private final Supplier<MediaIds> mediaIds;

    private final Duration leaseTime;

    private final Supplier<Instant> time;

    DefaultLeasesDispatcher(
        FbAuthenticator fbAuthenticator,
        Authorizer authorizer,
        LeasesRegistry leasesRegistry,
        Supplier<MediaIds> mediaIds,
        Duration leaseTime,
        Supplier<Instant> time
    ) {
        this.fbAuthenticator = requireNonNull(fbAuthenticator, "authenticator");
        this.authorizer = requireNonNull(authorizer, "authorizer");
        this.leasesRegistry = requireNonNull(leasesRegistry, "ticketOffice");
        this.mediaIds = requireNonNull(mediaIds, "mediaIds");
        this.leaseTime = requireNonNull(leaseTime, "ticketDuration");
        this.time = requireNonNull(time, "time");
    }

    @Override
    public Optional<LeasesActivation> currentLease(ExtAuthResponse extAuthResponse) {
        return currentOrRefreshed(extAuthResponse, false);
    }

    @Override
    public Optional<LeasesActivation> createLease(ExtAuthResponse extAuthResponse) {
        return currentOrRefreshed(extAuthResponse, true);
    }

    @Override
    public Optional<LeasesActivation> requestLease(LeasesRequest leasesRequest) {
        Instant time = this.time.get();
        return authorizer.login(leasesRequest.leasesData().userId(), false)
            .filter(userAuth ->
                userAuth.validAt(time))
            .flatMap(_ ->
                authorizer.authorize(userRequest(leasesRequest))
                    .map(authorized ->
                        requestedActivation(authorized, leasesRequest, time))
                    .map(this::storeActivated));
    }

    @Override
    public Optional<LeasesActivation> dismissLease(LeasesRequest leasesRequest) {
        return authorizer.deauthorize(userRequest(leasesRequest))
            .map(this::requestedDismissal)
            .map(this::storeActivated);
    }

    private Optional<LeasesActivation> currentOrRefreshed(ExtAuthResponse extAuthResponse, boolean refresh) {
        Instant time = this.time.get();
        return fbAuthenticator.authenticate(extAuthResponse).flatMap(auth ->
            authorizer.login(auth.id(), refresh)
                .filter(userAuth ->
                    userAuth.validAt(time))
                .map(userAuth ->
                    initialActivation(auth, userAuth, time))
                .map(this::storeActivated));
    }

    private LeasesActivation initialActivation(ExtUser fbUser, UserAuth userAuth, Instant time) {
        return new LeasesActivation(
            fbUser.name(),
            fbUser.id(),
            userAuth.token(),
            tracks(userAuth, time),
            userAuth.expiry()
        );
    }

    private LeasesActivation requestedActivation(UserAuth userAuth, LeasesRequest leasesRequest, Instant time) {
        return new LeasesActivation(
            null,
            leasesRequest.leasesData().userId(),
            leasesRequest.leasesData().token(),
            tracks(userAuth, time),
            userAuth.expiry()
        );
    }

    private LeasesActivation requestedDismissal(UserAuth deauthorized) {
        return new LeasesActivation(
            null,
            deauthorized.userId(),
            tracks(deauthorized, time.get()),
            deauthorized.expiry()
        );
    }

    private LeasesActivation storeActivated(LeasesActivation activation) {
        LeasePeriod leasePeriod = new LeasePeriod(time.get(), leaseTime);
        LeasesPath leasesPath = leasesRegistry.getActive(activation.token())
            .orElseGet(() ->
                leasesRegistry.setActive(new Leases(activation.token()), leasePeriod));
        LeasesPath activePath = leasesPath.withTracks(activation.trackUUIDs(), leasePeriod.getLapse());
        leasesRegistry.setActive(activePath.leases(), leasePeriod);
        return activation;
    }

    private List<Uuid> tracks(UserAuth userAuth, Instant time) {
        return userAuth.albumLeases()
            .stream()
            .filter(auth ->
                auth.validAt(time))
            .flatMap(this::trackUuids)
            .toList();
    }

    private Stream<Uuid> trackUuids(UserAuth.AlbumLease auth) {
        if (auth == null) {
            throw new IllegalArgumentException("Null auth");
        }
        List<Uuid> value = mediaIds.get().albumTracks().get(auth.albumId());
        return value == null
            ? Stream.empty()
            : value.stream();
    }

    private static UserRequest userRequest(LeasesRequest leasesRequest) {
        LeasesData data = leasesRequest.leasesData();
        return new UserRequest(data.userId(), data.token(), data.album());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + leasesRegistry + "]";
    }
}
