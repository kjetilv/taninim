package taninim.yellin;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.github.kjetilv.uplift.kernel.uuid.Uuid;
import taninim.fb.Authenticator;
import taninim.fb.ExtAuthResponse;
import taninim.fb.ExtUser;
import taninim.music.Leases;
import taninim.music.LeasesPath;
import taninim.music.LeasesRegistry;
import taninim.music.Period;
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
        Instant time = this.time.get();
        return authorizer.login(leasesRequest.userId(), false)
            .filter(userAuth ->
                userAuth.validAt(time))
            .flatMap(userAuth ->
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
            userAuth.token(),
            tracks(userAuth, time),
            userAuth.expiry()
        );
    }

    private LeasesActivation requestedActivation(UserAuth userAuth, LeasesRequest leasesRequest, Instant time) {
        return new LeasesActivation(
            null,
            leasesRequest.userId(),
            leasesRequest.token(),
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
        Period period = new Period(time.get(), leaseTime);
        LeasesPath leasesPath = leasesRegistry.getActive(activation.token())
            .orElseGet(() ->
                leasesRegistry.setActive(new Leases(activation.token()), period));
        LeasesPath activePath = leasesPath.withTracks(activation.trackUUIDs(), period.getLapse());
        leasesRegistry.setActive(activePath.leases(), period);
        return activation;
    }

    private List<Uuid> tracks(UserAuth userAuth, Instant time) {
        return userAuth.albumLeases().stream().filter(auth ->
                auth.validAt(time))
            .flatMap(this::trackUuids)
            .toList();
    }

    private Stream<Uuid> trackUuids(UserAuth.AlbumLease auth) {
        if (auth == null ) {
            throw new IllegalArgumentException("Null auth");
        }
        List<Uuid> value = mediaIds.get().albumTracks().get(auth.albumId());
        if (value == null) {
            return Stream.empty();
        }
        return value.stream();
    }

    private static UserRequest userRequest(LeasesRequest leasesRequest) {
        return new UserRequest(leasesRequest.userId(), leasesRequest.token(), leasesRequest.album());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + leasesRegistry + "]";
    }
}
