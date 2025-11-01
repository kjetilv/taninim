package taninim.yellin;

import module java.base;
import com.github.kjetilv.uplift.hash.HashKind;
import com.github.kjetilv.uplift.json.Json;
import com.github.kjetilv.uplift.json.mame.CachingJsonSessions;
import com.github.kjetilv.uplift.s3.S3Accessor;
import com.github.kjetilv.uplift.util.OnDemand;
import taninim.fb.FbAuthenticator;
import taninim.music.Archives;
import taninim.music.legal.ArchivedLeasesRegistry;
import taninim.music.legal.CloudMediaLibrary;
import taninim.music.legal.S3Archives;
import taninim.music.medias.MediaIds;
import taninim.music.medias.MediaLibrary;
import taninim.music.medias.UserAuths;

public final class Yellin {

    public static LeasesDispatcher leasesDispatcher(
        S3Accessor s3Accessor,
        Supplier<Instant> time,
        Duration sessionDuration,
        Duration ticketDuration,
        FbAuthenticator fbAuthenticator
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

    private static final Json JSON = Json.instance(CachingJsonSessions.create(HashKind.K128));

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
}
