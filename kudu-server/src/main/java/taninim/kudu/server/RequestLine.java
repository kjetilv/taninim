package taninim.kudu.server;

import module java.base;
import com.github.kjetilv.uplift.hash.Hash;
import com.github.kjetilv.uplift.hash.HashKind.K128;
import taninim.kudu.Track;

import static taninim.util.ParseBits.tailString;

record RequestLine(Track track, Hash<K128> token) {

    static Optional<RequestLine> parseRequestLine(String requestLine) {
        return tailString(requestLine, "get /audio/").flatMap(RequestLine::parse);
    }

    private static Optional<RequestLine> parse(String request) {
        return Track.parse(path(request))
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
            .map(Hash::<K128>from)
            .map(uuid -> new RequestLine(track, uuid));
    }

    private static String path(String request) {
        var queryIndex = request.indexOf("?t=");
        return queryIndex < 0
            ? request
            : request.substring(0, queryIndex);
    }
}
