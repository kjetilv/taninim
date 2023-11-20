package taninim.yellin.server;

import com.github.kjetilv.uplift.kernel.io.ByteBuffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taninim.fb.ExtAuthResponse;
import taninim.yellin.LeasesRequest;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.github.kjetilv.uplift.kernel.io.ParseBits.tailString;
import static java.util.Objects.requireNonNull;

record YellinRequest(
    ExtAuthResponse fbAuth,
    LeasesRequest request,
    Admin admin
) {

    private static final Logger log = LoggerFactory.getLogger(YellinRequest.class);

    static Optional<YellinRequest> read(
        ByteBuffer requestBuffer,
        Function<String, Map<?, ?>> jsonParser
    ) {
        try {
            return ByteBuffers.readBuffer(requestBuffer, nextLine ->
                nextLine.get()
                    .map(String::toLowerCase)
                    .flatMap(requestLine ->
                        handle(requestLine, nextLine, jsonParser)));
        } catch (Exception e) {
            log.warn("Failed to read auth request: {}", requestBuffer, e);
            return Optional.empty();
        }
    }

    YellinRequest(LeasesRequest leasesRequest) {
        this(null, leasesRequest, null);
    }

    private YellinRequest(ExtAuthResponse authResponse) {
        this(requireNonNull(authResponse, "fbAuth"), null, null);
    }

    boolean isPreflight() {
        return admin == Admin.preflight;
    }

    boolean isHealth() {
        return admin == Admin.health;
    }

    boolean isLeaseRequest() {
        return request != null;
    }

    boolean isAuthRequest() {
        return fbAuth != null;
    }

    private static final YellinRequest PREFLIGHT_REQ =
        new YellinRequest(null, null, Admin.preflight);

    private static final YellinRequest HEALTH_REQ =
        new YellinRequest(null, null, Admin.health);

    private static Optional<YellinRequest> handle(
        String requestLine, Supplier<Optional<String>> nextLine, Function<String, Map<?, ?>> jsonParser
    ) {
        return afterPrefix(requestLine, "post /auth")
            .flatMap(path ->
                readAuth(nextLine, jsonParser))
            .or(() ->
                afterPrefix(requestLine, "post /lease").flatMap(path ->
                    readLease(
                        nextLine,
                        body ->
                            LeasesRequest.acquire(body, jsonParser)
                    )))
            .or(() ->
                afterPrefix(requestLine, "delete /lease?")
                    .flatMap(LeasesRequest::release)
                    .map(YellinRequest::new))
            .or(() ->
                afterPrefix(requestLine, "options /").map(match ->
                    PREFLIGHT_REQ))
            .or(() ->
                afterPrefix(requestLine, "get /health", "head /health").map(match ->
                    HEALTH_REQ));
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
            .map(YellinRequest::new);
    }

    private static Optional<YellinRequest> readAuth(
        Supplier<Optional<String>> nextLine,
        Function<String, Map<?, ?>> jsonParser
    ) {
        skipHeaders(nextLine);
        return extractBody(nextLine).map(
            body ->
                new YellinRequest(ExtAuthResponse.from(body, jsonParser)));
    }

    private static Optional<String> extractBody(Supplier<Optional<String>> nextLine) {
        StringBuilder body = new StringBuilder();
        while (true) {
            Optional<String> json = nextLine.get();
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
            Optional<String> nonBlank = nextLine.get()
                .filter(str -> !str.isBlank());
            if (nonBlank.isEmpty()) {
                return;
            }
        }
    }

    private enum Admin {
        preflight,
        health
    }
}
