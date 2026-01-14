package taninim.yellin.server;

import module java.base;
import com.github.kjetilv.uplift.asynchttp.*;
import com.github.kjetilv.uplift.asynchttp.rere.HttpRequest;
import com.github.kjetilv.uplift.asynchttp.rere.HttpResponse;
import com.github.kjetilv.uplift.json.JsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taninim.fb.ExtAuthResponse;
import taninim.yellin.LeasesActivation;
import taninim.yellin.LeasesActivationRW;
import taninim.yellin.LeasesDispatcher;
import taninim.yellin.LeasesRequest;

import static com.github.kjetilv.uplift.asynchttp.Processing.*;
import static java.util.Objects.requireNonNull;

@SuppressWarnings("LoggingSimilarMessage")
class YellinSyncChannelHandler implements HttpSyncHandler.Server {

    private static final Logger log = LoggerFactory.getLogger(YellinSyncChannelHandler.class);

    private final LeasesDispatcher leasesDispatcher;

    YellinSyncChannelHandler(
        LeasesDispatcher leasesDispatcher,
        Supplier<Instant> time
    ) {
        this.time = time;
        this.leasesDispatcher = leasesDispatcher;
    }

    @Override
    public HttpResponse handle(HttpRequest httpReq, Instant instant) {
        var processing = YellinRequests.read(httpReq)
            .map(this::processing)
            .map(response -> )
        return new HttpResponse(
            switch (processing) {
                case REJECTED -> 400;
                case FAIL -> 500;
                case OK -> 200;
                case INCOMPLETE -> 100;
            },


        );
    }

    private HttpResponse processing(YellinRequest request) {
        log.debug("Processing {}", request);
        return switch (request) {
            case YellinRequest.Preflight _ -> handlePreflight(true);
            case YellinRequest.Health ignored -> handleHealth();
            case YellinRequest.Auth(var response) -> handleCurrentLease(response);
            case YellinRequest.Lease(var leasesRequest) -> handleNewLease(leasesRequest);
            case YellinRequest.Unknown unknown -> fail(unknown);
        };
    }

    private Processing handleCurrentLease(ExtAuthResponse extAuthResponse) {
        return leasesDispatcher.currentLease(extAuthResponse)
            .map(activation -> {
                log.debug("User {} has access to {} tracks", activation.name(), activation.size());
                writeResponse(activation);
                log.debug("Wrote back {}", activation);
                return OK;
            }).orElse(REJECTED);
    }

    private Processing handleNewLease(LeasesRequest leasesRequest) {
        return leasesDispatcher.requestLease(leasesRequest)
            .map(result -> {
                log.debug("User {} gets access to {} tracks", result.trackUUIDs().size(), result);
                writeResponse(result);
                log.debug("Wrote back {}", result);
                return OK;
            }).orElse(REJECTED);
    }

    private void writeResponse(LeasesActivation activation) {
        var writable = response(activation);
        try (var writer = responseWriter()) {
            writer.write(writable);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to respond: " + activation, e);
        }
    }

    private static final String CONTENT_HEADERS =
        """
            HTTP/1.1 200
            Access-Control-Allow-Origin: *
            Access-Control-Allow-Methods: GET, HEAD, POST, DELETE
            Content-Type: application/json
            Content-Length: %d
            Cache-Control: no-cache

            """;

    private static final JsonWriter<byte[], LeasesActivation, ByteArrayOutputStream> LEASES_ACT_WRITER =
        LeasesActivationRW.INSTANCE.bytesWriter();

    private static Writable<ByteBuffer> response(LeasesActivation activation) {
        var body = LEASES_ACT_WRITER.write(activation);
        var headers = jsonHeaders(body.length);
        var byteBuffer = ByteBuffer.wrap(append(headers, body));
        return new WritableBuffer<>(byteBuffer, byteBuffer.capacity());
    }

    private static byte[] jsonHeaders(int length) {
        return String.format(CONTENT_HEADERS, length).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] append(byte[]... parts) {
        var response = new byte[Arrays.stream(parts).mapToInt(part -> part.length).sum()];
        var offset = 0;
        for (var part : parts) {
            try {
                System.arraycopy(part, 0, response, offset, part.length);
            } finally {
                offset += part.length;
            }
        }
        return response;
    }

    private static Processing fail(YellinRequest req) {
        log.warn("Unhandled request: {}", req);
        return FAIL;
    }
}
