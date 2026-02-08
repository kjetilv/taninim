package taninim.yellin;

import module java.base;
import com.github.kjetilv.uplift.hash.Hash;
import com.github.kjetilv.uplift.hash.HashKind;
import com.github.kjetilv.uplift.hash.HashKind.K128;
import com.github.kjetilv.uplift.json.Json;
import com.github.kjetilv.uplift.json.mame.CachingJsonSessions;
import com.github.kjetilv.uplift.s3.S3Accessor;
import com.github.kjetilv.uplift.util.OnDemand;
import taninim.fb.Authenticator;
import taninim.fb.ExtAuthResponse;
import taninim.fb.ExtUser;
import taninim.music.LeasePeriod;
import taninim.music.Leases;
import taninim.music.LeasesRegistry;
import taninim.music.legal.ArchivedLeasesRegistry;
import taninim.music.legal.CloudMediaLibrary;
import taninim.music.legal.S3Archives;
import taninim.music.medias.*;

import static java.util.Objects.requireNonNull;

public final class DefaultYellin implements Yellin {

    public static Yellin create(
        S3Accessor s3Accessor,
        Supplier<Instant> time,
        Duration sessionDuration,
        Duration ticketDuration,
        Authenticator authenticator
    ) {
        var s3Archives = S3Archives.create(s3Accessor);
        var mediaLibrary = CloudMediaLibrary.create(s3Accessor, time);
        var leasesRegistry =
            ArchivedLeasesRegistry.create(s3Archives, ticketDuration, time);

        var onDemand = new OnDemand(time);

        var users =
            onDemand.<List<String>>after(REFRESH_INTERVAL).get(() ->
                ids(mediaLibrary));

        var mediaIds =
            onDemand.<MediaIds>after(REFRESH_INTERVAL).get(() ->
                mediaIds(mediaLibrary));

        var authIds =
            onDemand.<UserAuths>after(REFRESH_INTERVAL).get(() ->
                authIds(mediaLibrary));

        Consumer<UserAuths> updateAuthIds = userAuths ->
            authIds(mediaLibrary, userAuths);

        Consumer<UserAuths> forceUpdate = userAuths ->
            onDemand.force(authIds, userAuths);

        var authorizer = DefaultAuthorizer.create(
            authIds,
            updateAuthIds.andThen(forceUpdate),
            sessionDuration,
            ticketDuration,
            time
        );

        Authenticator userAuthenticator = authResponse ->
            authenticator.authenticate(authResponse)
                .filter(extUser ->
                    users.get().contains(extUser.id()));

        return new DefaultYellin(
            userAuthenticator,
            authorizer,
            leasesRegistry,
            mediaIds,
            ticketDuration,
            time
        );
    }

    private final Authenticator authenticator;

    private final Authorizer authorizer;

    private final LeasesRegistry leasesRegistry;

    private final Supplier<MediaIds> mediaIds;

    private final Duration leaseTime;

    private final Supplier<Instant> time;

    private DefaultYellin(
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

    static final Json JSON = Json.instance(CachingJsonSessions.create(HashKind.K128));

    private static final TemporalAmount REFRESH_INTERVAL = Duration.ofHours(1);

    private static UserRequest userRequest(LeasesRequest leasesRequest) {
        var data = leasesRequest.leasesData();
        return new UserRequest(
            data.userId(),
            Hash.from(data.token()),
            Hash.from(data.album())
        );
    }

    private static MediaIds mediaIds(MediaLibrary mediaLibrary) {
        return mediaLibrary.stream("media-digest.bin")
            .map(inputStream ->
                MediaIds.from(new DataInputStream(inputStream)))
            .orElseGet(MediaIds::new);
    }

    private static UserAuths authIds(MediaLibrary mediaLibrary) {
        return mediaLibrary.stream("auth-digest.bin")
            .map(inputStream ->
                UserAuths.from(new DataInputStream(inputStream)))
            .orElseGet(UserAuths::new);
    }

    @SuppressWarnings("unchecked")
    private static List<String> ids(MediaLibrary mediaLibrary) {
        return mediaLibrary.stream("ids.json")
            .map(inputStream -> {
                var acls = (Map<?, ?>) JSON.read(inputStream);
                var acl = (List<Map<String, Object>>) acls.get("acl");
                return acl.stream()
                    .map(map ->
                        map.get("ser"))
                    .map(String::valueOf)
                    .toList();
            }).orElseGet(Collections::emptyList);
    }

    private static void authIds(MediaLibrary mediaLibrary, UserAuths userAuths) {
        mediaLibrary.write(
            "auth-digest.bin", outputStream -> {
                try (var dos = new DataOutputStream(outputStream)) {
                    userAuths.writeTo(dos);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to write " + userAuths, e);
                }
            }
        );
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + leasesRegistry + "]";
    }
}
