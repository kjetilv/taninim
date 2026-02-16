package taninim.kudu.server;

import com.github.kjetilv.uplift.hash.Hash;
import com.github.kjetilv.uplift.hash.HashKind;
import com.github.kjetilv.uplift.kernel.io.Range;
import com.github.kjetilv.uplift.synchttp.rere.HttpReq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taninim.kudu.Track;

import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

public sealed interface KuduRequest {

    Logger log = LoggerFactory.getLogger(KuduRequest.class);

    KuduRequest PREFLIGHT_REQ = new Preflight();

    KuduRequest HEALTH_REQ = new Health();

    MemorySegment RANGE = MemorySegment.ofArray("range".getBytes(StandardCharsets.UTF_8));

    static Optional<? extends KuduRequest> from(HttpReq httpReq) {
        return switch (httpReq.method()) {
            case GET -> switch (match(httpReq.path(), "/library.jsonl", "/audio/")) {
                case "/library.jsonl" -> Optional.of(new Library(token(httpReq)));
                case "/audio/" -> audio(httpReq);
                default -> Optional.empty();
            };
            case HEAD -> switch (match(httpReq.path(), "/health", "")) {
                case "/health" -> Optional.of(HEALTH_REQ);
                case "" -> Optional.of(PREFLIGHT_REQ);
                default -> Optional.empty();
            };
            case OPTIONS -> Optional.of(PREFLIGHT_REQ);
            default -> Optional.empty();
        };
    }

    private static Optional<Audio> audio(HttpReq httpReq) {
        try {
            var req = httpReq.withQueryParameters();
            var file = req.path("/audio/");
            var dotIndex = req.path().lastIndexOf('.');
            var trackUUID = Hash.fromUuid(UUID.fromString(file.substring(0, dotIndex)));
            var format = Track.Format.valueOf(file.substring(dotIndex + 1));
            var track = new Track(trackUUID, format);
            return Optional.ofNullable(req.headers().header(RANGE))
                .flatMap(header ->
                    Range.read(header)
                        .map(range ->
                            new Audio(track, range, token(req))));
        } catch (Exception e) {
            log.warn("Failed to parse track range from {}", httpReq, e);
            return Optional.empty();
        }
    }

    private static String match(String path, String... prefixes) {
        return Arrays.stream(prefixes)
            .filter(path::startsWith)
            .findFirst()
            .orElse(null);
    }

    private static Hash<HashKind.K128> token(HttpReq httpReq) {
        return Hash.from(httpReq.queryParameters().par("t"));
    }

    record Library(Hash<HashKind.K128> token) implements KuduRequest {
    }

    record Audio(Track track, Range range, Hash<HashKind.K128> token) implements KuduRequest {
    }

    record Preflight() implements KuduRequest {
    }

    record Health() implements KuduRequest {
    }
}
