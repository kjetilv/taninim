package taninim.yellin;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.github.kjetilv.uplift.json.Json;
import com.github.kjetilv.uplift.kernel.util.OnDemand;
import com.github.kjetilv.uplift.s3.S3Accessor;
import taninim.fb.Authenticator;
import taninim.music.LeasesRegistry;
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
        Authenticator authenticator
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

        Authenticator userAuthenticator = authResponse ->
            authenticator.authenticate(authResponse).filter(extUser ->
                users.get().contains(extUser.id()));

        return new DefaultLeasesDispatcher(
            userAuthenticator,
            authorizer,
            leasesRegistry,
            mediaIds,
            ticketDuration,
            time
        );
    }

    public static ActivationSerializer activationSerializer(S3Accessor s3Accessor) {
        return new ActivationSerializer(Json.OBJECT_2_STRING, s3Accessor::presign);
    }

    private Yellin() {

    }

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
        return mediaLibrary.stream("ids.json").map(inputStream -> {
            Map<String, Object> acls = Json.BYTES_2_JSON_MAP.apply(inputStream);
            List<Map<String, Object>> acl = (List<Map<String, Object>>) acls.get("acl");
            return acl.stream().map(map -> map.get("ser")).map(String::valueOf).toList();
        }).orElseGet(Collections::emptyList);
    }

    private static void authIds(MediaLibrary mediaLibrary, UserAuths userAuths) {
        mediaLibrary.write("auth-digest.bin", outputStream -> {
            try (DataOutputStream dos = new DataOutputStream(outputStream)) {
                userAuths.writeTo(dos);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to write " + userAuths, e);
            }
        });
    }
}
