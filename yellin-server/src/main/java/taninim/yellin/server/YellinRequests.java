package taninim.yellin.server;

import module java.base;
import com.github.kjetilv.uplift.asynchttp.rere.HttpRequest;
import com.github.kjetilv.uplift.json.FieldEvents;
import com.github.kjetilv.uplift.json.JsonReader;
import com.github.kjetilv.uplift.kernel.io.ByteBuffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taninim.fb.ExtAuthResponse;
import taninim.fb.ExtAuthResponseRW;
import taninim.yellin.LeasesRequest;

import static taninim.util.ParseBits.tailString;

final class YellinRequests {

    public static final Logger log = LoggerFactory.getLogger(YellinRequests.class);

    public static final JsonReader<String, ExtAuthResponse> EXT_AUTH_READER =
        ExtAuthResponseRW.INSTANCE.stringReader();

    public static Optional<YellinRequest> read(ByteBuffer requestBuffer) {
        try {
            return ByteBuffers.readBuffer(
                requestBuffer, nextLine ->
                    nextLine.get()
                        .map(String::toLowerCase)
                        .flatMap(requestLine ->
                            handle(requestLine, nextLine))
            );
        } catch (Exception e) {
            log.warn("Failed to read auth request: {}", requestBuffer, e);
            return Optional.empty();
        }
    }

    public static Optional<YellinRequest> read(HttpRequest httpReq) {
        return null;
    }

    private YellinRequests() {
    }

    private static Optional<YellinRequest> handle(
        String requestLine, Supplier<Optional<String>> nextLine
    ) {
        return afterPrefix(requestLine, "post /auth")
            .flatMap(_ -> readAuth(nextLine))
            .or(() ->
                afterPrefix(requestLine, "post /lease").flatMap(_ -> readLease(
                    nextLine,
                    LeasesRequest::acquire
                )))
            .or(() ->
                afterPrefix(requestLine, "delete /lease?")
                    .flatMap(LeasesRequest::releaseQueryPars)
                    .map(YellinRequest.Lease::new))
            .or(() ->
                afterPrefix(requestLine, "options /").map(_ -> YellinRequest.PREFLIGHT_REQ))
            .or(() ->
                afterPrefix(requestLine, "get /health", "head /health").map(_ -> YellinRequest.HEALTH_REQ));
    }

    private static Optional<String> afterPrefix(String line, String... prefices) {
        return Arrays.stream(prefices)
            .flatMap(prefix ->
                tailString(line, prefix).stream())
            .findFirst();
    }

    private static Optional<YellinRequest> readLease(
        Supplier<Optional<String>> nextLine,
        Function<String, LeasesRequest> toRequest
    ) {
        skipHeaders(nextLine);
        return extractBody(nextLine)
            .map(toRequest)
            .map(YellinRequest.Lease::new);
    }

    private static Optional<YellinRequest> readAuth(Supplier<Optional<String>> nextLine) {
        skipHeaders(nextLine);
        return extractBody(nextLine).map(body ->
            new YellinRequest.Auth(EXT_AUTH_READER.read(body)));
    }

    private static Optional<String> extractBody(Supplier<Optional<String>> nextLine) {
        var body = new StringBuilder();
        while (true) {
            var json = nextLine.get();
            if (json.isEmpty()) {
                return Optional.of(body)
                    .filter(sb -> !sb.isEmpty())
                    .map(StringBuilder::toString);
            }
            json.ifPresent(str ->
                body.append(str)
                    .append("\n"));
        }
    }

    private static void skipHeaders(Supplier<Optional<String>> nextLine) {
        while (true) {
            var nonBlank = nextLine.get()
                .filter(str -> !str.isBlank());
            if (nonBlank.isEmpty()) {
                return;
            }
        }
    }
}
