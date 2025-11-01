package taninim.kudu.server;

import module java.base;
import com.github.kjetilv.uplift.uuid.Uuid;
import taninim.kudu.Track;

import static taninim.util.ParseBits.tailString;

record RequestLine(Track track, Uuid token) {

    static Optional<RequestLine> parseRequestLine(String requestLine) {
        return tailString(requestLine, "get /audio/").flatMap(RequestLine::parse);
    }

    private static Optional<RequestLine> parse(String request) {
        return Track.parse(request)
            .flatMap(track ->
                requestedTrack(request, track));
    }

    private static Optional<RequestLine> requestedTrack(String request, Track track) {
        var queryIndex = request.lastIndexOf("?t=");
        if (queryIndex < 0) {
            return Optional.empty();
        }
        var queryStart = queryIndex + "?t=".length();
        return tailString(request, queryStart)
            .flatMap(Uuid::maybeFrom)
            .map(uuid ->
                new RequestLine(track, uuid));
    }
}
