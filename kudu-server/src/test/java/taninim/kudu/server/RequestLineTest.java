package taninim.kudu.server;

import module java.base;
import com.github.kjetilv.uplift.hash.HashKind;
import org.junit.jupiter.api.Test;
import taninim.kudu.Track;

import static com.github.kjetilv.uplift.hash.HashKind.K128;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestLineTest {

    @Test
    void parseLambda() {
        var token = K128.random();
        var trackId = HashKind.K128.random();
        var requestLine = "get /audio/%s.m4a?t=%s".formatted(trackId.digest(), token.digest());
        var track = RequestLine.parseRequestLine(requestLine);
        assertTrue(track.isPresent());
        assertEquals(new RequestLine(new Track(trackId, Track.Format.M4A), token), track.get());
    }
}
