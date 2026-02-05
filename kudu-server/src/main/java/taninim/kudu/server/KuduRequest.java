package taninim.kudu.server;

import com.github.kjetilv.uplift.hash.Hash;
import com.github.kjetilv.uplift.hash.HashKind;
import com.github.kjetilv.uplift.kernel.io.Range;
import com.github.kjetilv.uplift.synchttp.req.HttpReq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taninim.kudu.Track;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public sealed interface KuduRequest {

    Logger log = LoggerFactory.getLogger(KuduRequest.class);

    KuduRequest PREFLIGHT_REQ = new Preflight();

    KuduRequest HEALTH_REQ = new Health();

    static Optional<? extends KuduRequest> from(HttpReq httpReq) {
        return switch (httpReq.method()) {
            case OPTIONS -> Optional.of(PREFLIGHT_REQ);
            case HEAD -> httpReq.path().startsWith("/health")
                ? Optional.of(HEALTH_REQ)
                : Optional.of(PREFLIGHT_REQ);
            case GET -> switch (match(httpReq.path())) {
                case "/library.json" -> Optional.of(new Library(token(httpReq)));
                case "/audio/" -> trackRange(httpReq);
                default -> Optional.empty();
            };
            default -> Optional.empty();
        };
    }

    private static Optional<Audio> trackRange(HttpReq httpReq) {
        var req = httpReq.withQueryParameters();
        try {
            var file = req.path("/audio/");
            var dotIndex = req.path().lastIndexOf('.');
            var uuid = UUID.fromString(file.substring(0, dotIndex));
            return Optional.of(new Audio(
                new Track(
                    Hash.fromUuid(uuid),
                    Track.Format.valueOf(file.substring(dotIndex + 1))
                ),
                req.headers().header("range").flatMap(Range::read).orElseThrow(() ->
                    new IllegalStateException("Failed to parse range: " + req.headers())),
                token(req)
            ));
        } catch (Exception e) {
            log.warn("Failed to parse track range from {}", req.path(), e);
            return Optional.empty();
        }
    }

    private static String match(String path) {
        return Stream.of(
                "/library.json",
                "/audio/"
            )
            .filter(path::startsWith)
            .findFirst()
            .orElse(null);
    }

    private static Hash<HashKind.K128> token(HttpReq httpReq) {
        return Hash.from(httpReq.queryParameters().par("t"));
    }

    record Library(Hash<HashKind.K128> token) implements KuduRequest {
    }

    record Audio(
        Track track,
        Range range,
        Hash<HashKind.K128> token
    ) implements KuduRequest {
    }

    record Preflight() implements KuduRequest {
    }

    record Health() implements KuduRequest {
    }
}
