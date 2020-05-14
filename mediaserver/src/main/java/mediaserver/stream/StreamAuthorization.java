package mediaserver.stream;

import mediaserver.GlobalState;
import mediaserver.http.Req;
import mediaserver.media.AlbumTrack;
import mediaserver.media.Media;
import mediaserver.media.Track;
import mediaserver.sessions.AccessLevel;
import mediaserver.util.Pair;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class StreamAuthorization {

    private StreamAuthorization() {
    }

    public static boolean authorized(Media media, Req req, AlbumTrack albumTrack) {

        return authorized(req, albumTrack, () -> media);
    }

    static boolean authorized(Req req, AlbumTrack albumTrack, Supplier<Media> media) {

        AccessLevel accessLevel = req.getAccessLevel();
        if (singlePlayableTrack(req).anyMatch(albumTrack.getTrack()::equals)) {
            return accessLevel.satisfies(AccessLevel.STREAM_SINGLE);
        }
        return accessLevel.satisfies(AccessLevel.STREAM) ||
            accessLevel.satisfies(AccessLevel.STREAM_CURATED) && media.get().isCurated(albumTrack);
    }

    private static Stream<Track> singlePlayableTrack(Req req) {

        return Stream.of(
            GlobalState.get().getGlobalTrack(req.getTime()),
            req.getSession().getSessionState().getRandomTrack(req.getTime())
        ).flatMap(Optional::stream).map(Pair::getT2);
    }
}
