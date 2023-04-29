package mediaserver.kudu.server;

import java.util.Optional;

import com.github.kjetilv.uplift.kernel.io.ParseBits;
import com.github.kjetilv.uplift.kernel.uuid.Uuid;
import mediaserver.kudu.Track;

import static com.github.kjetilv.uplift.kernel.io.ParseBits.tailString;

record RequestLine(
    Track track,
    Uuid token
) {

    static Optional<RequestLine> parseRequestLine(String requestLine) {
        return ParseBits.tailString(requestLine, PREFIX).flatMap(RequestLine::parsePath);
    }

    private static final String PREFIX = "get /audio/";

    private static final String QUERY = "?t=";

    private static Optional<RequestLine> parsePath(String request) {
        Optional<Track> track1 = Track.parseTrack(request);
        return track1.flatMap(track ->
            Optional.of(request.lastIndexOf(QUERY))
                .filter(index -> index >= 0)
                .map(index -> index + QUERY.length())
                .flatMap(index -> tailString(request, index))
                .flatMap(Uuid::maybeFrom)
                .map(uuid ->
                    new RequestLine(track, uuid)));
    }
}
