package taninim.yellin;

import module java.base;
import module taninim.fb;
import module taninim.taninim;
import module uplift.hash;
import module uplift.json;
import module uplift.json.mame;
import module uplift.s3;
import module uplift.util;

public final class Yellin {

    public static LeasesDispatcher leasesDispatcher(
        S3Accessor s3Accessor,
        Supplier<Instant> time,
        Duration sessionDuration,
        Duration ticketDuration,
        FbAuthenticator fbAuthenticator
    ) {
        S3Archives s3Archives = new S3Archives(s3Accessor);
        MediaLibrary mediaLibrary = new CloudMediaLibrary(s3Accessor, time);
        LeasesRegistry leasesRegistry =
            new ArchivedLeasesRegistry(s3Archives, ticketDuration, time);

        OnDemand onDemand = new OnDemand(time);

        Supplier<List<String>> users =
            onDemand.<List<String>>after(REFRESH_INTERVAL).get(() ->
                ids(mediaLibrary));

        Supplier<MediaIds> mediaIds =
            onDemand.<MediaIds>after(REFRESH_INTERVAL).get(() ->
                mediaIds(mediaLibrary));

        Supplier<UserAuths> authIds =
            onDemand.<UserAuths>after(REFRESH_INTERVAL).get(() ->
                authIds(mediaLibrary));

        Consumer<UserAuths> updateAuthIds = userAuths -> authIds(mediaLibrary, userAuths);
        Consumer<UserAuths> forceUpdate = userAuths -> onDemand.force(authIds, userAuths);

        Authorizer authorizer = new DefaultAuthorizer(
            authIds,
            updateAuthIds.andThen(forceUpdate),
            sessionDuration,
            ticketDuration,
            time
        );

        FbAuthenticator userFbAuthenticator = authResponse ->
            fbAuthenticator.authenticate(authResponse)
                .filter(extUser ->
                    users.get().contains(extUser.id()));

        return new DefaultLeasesDispatcher(
            userFbAuthenticator,
            authorizer,
            leasesRegistry,
            mediaIds,
            ticketDuration,
            time
        );
    }

    private Yellin() {
    }

    private static final Json JSON  = Json.instance(CachingJsonSessions.create(HashKind.K128));

    private static final TemporalAmount REFRESH_INTERVAL = Duration.ofHours(1);

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
                Map<?, ?> acls = (Map<?, ?>) JSON.read(inputStream);
                List<Map<String, Object>> acl = (List<Map<String, Object>>) acls.get("acl");
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
                try (DataOutputStream dos = new DataOutputStream(outputStream)) {
                    userAuths.writeTo(dos);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to write " + userAuths, e);
                }
            }
        );
    }
}
