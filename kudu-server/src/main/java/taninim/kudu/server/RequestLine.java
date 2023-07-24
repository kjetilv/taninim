package taninim.kudu.server;

import java.util.Optional;

import com.github.kjetilv.uplift.kernel.uuid.Uuid;
import taninim.kudu.Track;

import static com.github.kjetilv.uplift.kernel.io.ParseBits.tailString;

record RequestLine(
    Track track,
    Uuid token
) {

    static Optional<RequestLine> parseRequestLine(String requestLine) {
        return tailString(requestLine, "get /audio/").flatMap(RequestLine::parse);
    }

    private static Optional<RequestLine> parse(String request) {
        return Track.parseTrack(request)
            .flatMap(track ->
                requestedTrack(request, track));
    }

    private static Optional<RequestLine> requestedTrack(String request, Track track) {
        int queryIndex = request.lastIndexOf("?t=");
        if (queryIndex < 0) {
            return Optional.empty();
        }
        int queryStart = queryIndex + "?t=".length();
        return tailString(request, queryStart)
            .flatMap(Uuid::maybeFrom)
            .map(uuid ->
                new RequestLine(track, uuid));
    }
}
