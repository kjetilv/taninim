package mediaserver.gui;

import io.netty.handler.codec.http.HttpResponse;
import mediaserver.http.WebPath;
import mediaserver.media.Media;
import mediaserver.media.Track;
import mediaserver.sessions.AccessLevel;

import java.util.function.Supplier;

public interface Streamer {

    HttpResponse stream(WebPath webPath, Track track);

    static boolean isAuthorized(WebPath webPath, Track track, Media media) {
        return isAuthorized(webPath, track, () -> media);
    }

    static boolean isAuthorized(WebPath webPath, Track track, Supplier<Media> media) {

        AccessLevel accessLevel = webPath.getAccessLevel();
        return accessLevel.satisfies(AccessLevel.STREAM) ||
            accessLevel.satisfies(AccessLevel.STREAM_CURATED) && media.get().isCurated(track);
    }
}
