package taninim.kudu.server;

import module java.base;
import com.github.kjetilv.uplift.uuid.Uuid;
import org.junit.jupiter.api.Test;
import taninim.kudu.Track;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestLineTest {

    @Test
    void parseLambda() {
        var token = Uuid.random();
        var trackId = Uuid.random();
        var requestLine = "get /audio/%s.m4a?t=%s".formatted(trackId.digest(), token.digest());
        var track = RequestLine.parseRequestLine(requestLine);
        assertTrue(track.isPresent());
        assertEquals(new RequestLine(new Track(trackId, Track.Format.M4A), token), track.get());
    }
}
