package taninim.kudu.server;

import module java.base;
import com.github.kjetilv.uplift.kernel.io.ByteBuffers;
import com.github.kjetilv.uplift.kernel.io.Range;
import com.github.kjetilv.uplift.uuid.Uuid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taninim.kudu.TrackRange;
import taninim.util.ParseBits;

import static taninim.util.ParseBits.tailString;

record ReceivedRequest(TrackRange trackRange, LibraryRequest libraryRequest, Admin admin) {

    private static final Logger log = LoggerFactory.getLogger(ReceivedRequest.class);

    static Optional<ReceivedRequest> fromHttp(ByteBuffer byteBuffer) {
        return ByteBuffers.readBuffer(byteBuffer, nextLine -> {
            try {
                return nextLine.get().map(String::trim)
                    .filter(NON_EMPTY)
                    .map(line ->
                        receivedRequest(line, nextLine))
                    .or(() ->
                        Optional.of(RETRY_REQ));
            } catch (Exception e) {
                log.error("Failed to parse request", e);
                return Optional.empty();
            }
        });
    }

    public boolean library() {
        return libraryRequest != null;
    }

    boolean health() {
        return admin == Admin.health;
    }

    boolean reject() {
        return admin == Admin.reject;
    }

    boolean retry() {
        return admin == Admin.retry;
    }

    boolean preflight() {
        return admin == Admin.preflight;
    }

    Optional<TrackRange> optionalTrackRange() {
        return Optional.ofNullable(trackRange);
    }

    private static final Predicate<String> NON_EMPTY = line -> !line.isBlank();

    private static final int MAX_HEADERS = 20;

    private static final ReceivedRequest PREFLIGHT_REQ =
        new ReceivedRequest(null, null, Admin.preflight);

    private static final ReceivedRequest RETRY_REQ =
        new ReceivedRequest(null, null, Admin.retry);

    private static final ReceivedRequest HEALTH_REQ =
        new ReceivedRequest(null, null, Admin.health);

    private static final ReceivedRequest REJECT_REQ =
        new ReceivedRequest(null, null, Admin.reject);

    private static ReceivedRequest receivedRequest(String line, Supplier<Optional<String>> nextLine) {
        var lc = line.toLowerCase(Locale.ROOT);
        return lc.startsWith("get /audio/") ? trackRequest(line, nextLine)
            : lc.startsWith("options /") ? PREFLIGHT_REQ
                : lc.startsWith("get /health") || lc.startsWith("head /health") ? HEALTH_REQ
                    : lc.startsWith("get /library.json") ? libraryRequest(line)
                        : REJECT_REQ;
    }

    private static ReceivedRequest trackRequest(String line, Supplier<Optional<String>> nextLine) {
        return RequestLine.parseRequestLine(line)
            .flatMap(requestLine ->
                trackRequest(requestLine, nextLine))
            .orElse(REJECT_REQ);
    }

    private static ReceivedRequest libraryRequest(String line) {
        return tailString(line, "get /library.json?t=")
            .map(token ->
                new LibraryRequest(Uuid.from(token)))
            .map(libraryRequest ->
                new ReceivedRequest(null, libraryRequest, null))
            .orElse(REJECT_REQ);
    }

    private static Optional<ReceivedRequest> trackRequest(
        RequestLine requestLine,
        Supplier<Optional<String>> nextLine
    ) {
        var headersParsed = 0;
        while (true) {
            var rawHeader = nextLine.get();
            if (rawHeader.isEmpty()) {
                return Optional.of(RETRY_REQ);
            }
            var headerLine = rawHeader.map(ReceivedRequest::toLowerCase);
            var header = headerLine.flatMap(ReceivedRequest::header);
            headersParsed++;
            var rangeHeader = header.filter(ReceivedRequest::isRange).map(Map.Entry::getValue);
            if (rangeHeader.isPresent()) {
                var arg = rangeHeader.get();
                return rangeHeader.flatMap(Range::read)
                    .map(validRange ->
                        new TrackRange(requestLine.track(), validRange, requestLine.token()))
                    .map(trackRange ->
                        new ReceivedRequest(trackRange, null, null))
                    .or(() -> {
                        log.debug("Invalid range header encountered: {}", arg);
                        return Optional.of(REJECT_REQ);
                    });
            }
            if (headersParsed >= MAX_HEADERS) {
                log.debug("Parsed {} headers, rejecting", headersParsed);
                return Optional.of(REJECT_REQ);
            }
        }
    }

    private static boolean isRange(Map.Entry<String, String> entry) {
        return entry.getKey().equals("range");
    }

    private static String toLowerCase(String s) {
        return s.toLowerCase(Locale.ROOT);
    }

    private static Optional<Map.Entry<String, String>> header(String line) {
        var index = line.indexOf(':');
        if (index < 0) {
            return Optional.empty();
        }
        return ParseBits
            .headString(line, index)
            .map(String::trim)
            .flatMap(head ->
                tailString(line, index + 1)
                    .map(String::trim)
                    .filter(NON_EMPTY)
                    .map(tail -> Map.entry(head, tail)));
    }

    private enum Admin {
        retry,
        reject,
        preflight,
        health
    }
}

