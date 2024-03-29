package taninim.kudu.server;

import java.util.Optional;

import com.github.kjetilv.uplift.uuid.Uuid;
import org.junit.jupiter.api.Test;
import taninim.kudu.Track;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestLineTest {

    @Test
    void parseLambda() {
        Uuid token = Uuid.random();
        Uuid trackId = Uuid.random();
        String requestLine = "get /audio/%s.m4a?t=%s".formatted(trackId.digest(), token.digest());
        Optional<RequestLine> track = RequestLine.parseRequestLine(requestLine);
        assertTrue(track.isPresent());
        assertEquals(new RequestLine(new Track(trackId, Track.Format.M4A), token), track.get());
    }
}
