package mediaserver.stream;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import mediaserver.GlobalState;
import mediaserver.http.Req;
import mediaserver.media.AlbumTrack;
import mediaserver.media.Media;
import mediaserver.media.Track;
import mediaserver.util.Pair;

import static mediaserver.sessions.AccessLevel.STREAM;
import static mediaserver.sessions.AccessLevel.STREAM_CURATED;
import static mediaserver.sessions.AccessLevel.STREAM_SINGLE;

public final class StreamAuthorization {

    private StreamAuthorization() {
    }

    public static boolean authorizedStreaming(Media media, Req req, AlbumTrack albumTrack) {
        return authorizedStreaming(req, albumTrack, () -> media);
    }

    static boolean authorizedStreaming(Req req, AlbumTrack albumTrack, Supplier<? extends Media> media) {
        if (singlePlayableTrack(req).anyMatch(albumTrack.getTrack()::equals)) {
            return req.hasAccess(STREAM_SINGLE);
        }
        return req.hasAccess(STREAM) || req.hasAccess(STREAM_CURATED) && media.get().isCurated(albumTrack);
    }

    private static Stream<Track> singlePlayableTrack(Req req) {
        return Stream.of(
            GlobalState.get().getGlobalTrack(req.getTime()),
            req.getSession().getSessionState().getRandomTrack(req.getTime())
        ).flatMap(Optional::stream).map(Pair::getT2);
    }
}
