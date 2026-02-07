package taninim.yellin;

import module java.base;
import com.github.kjetilv.uplift.hash.Hash;
import com.github.kjetilv.uplift.hash.HashKind.K128;
import taninim.fb.Authenticator;
import taninim.fb.ExtAuthResponse;
import taninim.fb.ExtUser;
import taninim.music.LeasePeriod;
import taninim.music.Leases;
import taninim.music.LeasesRegistry;
import taninim.music.medias.MediaIds;
import taninim.music.medias.UserAuth;
import taninim.music.medias.UserRequest;

import static java.util.Objects.requireNonNull;

public class DefaultLeasesDispatcher implements LeasesDispatcher {

    private final Authenticator authenticator;

    private final Authorizer authorizer;

    private final LeasesRegistry leasesRegistry;

    private final Supplier<MediaIds> mediaIds;

    private final Duration leaseTime;

    private final Supplier<Instant> time;

    DefaultLeasesDispatcher(
        Authenticator authenticator,
        Authorizer authorizer,
        LeasesRegistry leasesRegistry,
        Supplier<MediaIds> mediaIds,
        Duration leaseTime,
        Supplier<Instant> time
    ) {
        this.authenticator = requireNonNull(authenticator, "authenticator");
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
        var time = this.time.get();
        return authorizer.login(leasesRequest.leasesData().userId(), false)
            .filter(userAuth ->
                userAuth.validAt(time))
            .flatMap(_ -> {
                var userRequest = userRequest(leasesRequest);
                return authorizer.authorize(userRequest)
                    .map(authorized ->
                        requestedActivation(authorized, leasesRequest, time))
                    .map(this::storeActivated);
            });
    }

    @Override
    public Optional<LeasesActivation> dismissLease(LeasesRequest leasesRequest) {
        return authorizer.deauthorize(userRequest(leasesRequest))
            .map(this::requestedDismissal)
            .map(this::storeActivated);
    }

    private Optional<LeasesActivation> currentOrRefreshed(ExtAuthResponse extAuthResponse, boolean refresh) {
        var time = this.time.get();
        return authenticator.authenticate(extAuthResponse).flatMap(auth ->
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
            userAuth.token().digest(),
            tracks(userAuth, time).stream()
                .map(Hash::digest)
                .toList(),
            userAuth.expiry()
        );
    }

    private LeasesActivation requestedActivation(UserAuth userAuth, LeasesRequest leasesRequest, Instant time) {
        return new LeasesActivation(
            null,
            leasesRequest.leasesData().userId(),
            leasesRequest.leasesData().token(),
            tracks(userAuth, time).stream()
                .map(Hash::digest)
                .toList(),
            userAuth.expiry()
        );
    }

    private LeasesActivation requestedDismissal(UserAuth deauthorized) {
        return new LeasesActivation(
            null,
            deauthorized.userId(),
            tracks(deauthorized, time.get()).stream()
                .map(Hash::digest)
                .toList(),
            deauthorized.expiry()
        );
    }

    private LeasesActivation storeActivated(LeasesActivation activation) {
        var leasePeriod = new LeasePeriod(time.get(), leaseTime);
        var leasesPath = leasesRegistry.getActive(Hash.from(activation.token()))
            .orElseGet(() ->
                leasesRegistry.setActive(new Leases(Hash.from(activation.token())), leasePeriod));
        var list = activation.trackUUIDs()
            .stream()
            .<Hash<K128>>map(Hash::from)
            .toList();
        var activePath = leasesPath.withTracks(
            list,
            leasePeriod.getLapse()
        );
        leasesRegistry.setActive(activePath.leases(), leasePeriod);
        return activation;
    }

    private List<Hash<K128>> tracks(UserAuth userAuth, Instant time) {
        return userAuth.albumLeases()
            .stream()
            .filter(auth ->
                auth.validAt(time))
            .flatMap(this::trackUuids)
            .toList();
    }

    private Stream<Hash<K128>> trackUuids(UserAuth.AlbumLease auth) {
        if (auth == null) {
            throw new IllegalArgumentException("Null auth");
        }
        var albumTracks = mediaIds.get().albumTracks();
        var value = albumTracks.get(auth.albumId());
        return value == null
            ? Stream.empty()
            : value.stream();
    }

    private static UserRequest userRequest(LeasesRequest leasesRequest) {
        var data = leasesRequest.leasesData();
        return new UserRequest(
            data.userId(),
            Hash.from(data.token()),
            Hash.from(data.album())
        );
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + leasesRegistry + "]";
    }
}
