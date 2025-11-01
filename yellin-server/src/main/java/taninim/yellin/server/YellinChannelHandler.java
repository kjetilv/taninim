package taninim.yellin.server;

import module java.base;
import com.github.kjetilv.uplift.asynchttp.*;
import com.github.kjetilv.uplift.json.JsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taninim.fb.ExtAuthResponse;
import taninim.yellin.LeasesActivation;
import taninim.yellin.LeasesActivationRW;
import taninim.yellin.LeasesDispatcher;
import taninim.yellin.LeasesRequest;

import static java.util.Objects.requireNonNull;

@SuppressWarnings("LoggingSimilarMessage")
class YellinChannelHandler extends BufferStateChannelHandler<YellinChannelHandler> {

    private static final Logger log = LoggerFactory.getLogger(YellinChannelHandler.class);

    private final LeasesDispatcher leasesDispatcher;

    YellinChannelHandler(
        LeasesDispatcher leasesDispatcher,
        AsynchronousByteChannel channel,
        int maxRequestLength,
        Supplier<Instant> time
    ) {
        super(channel, maxRequestLength, time);
        this.leasesDispatcher = leasesDispatcher;
    }

    @Override
    public YellinChannelHandler bind(AsynchronousByteChannel channel) {
        return new YellinChannelHandler(
            leasesDispatcher,
            requireNonNull(channel, "channel"),
            maxRequestLength(),
            clock()
        );
    }

    @Override
    protected Processing process(BufferState state) {
        return YellinRequests.read(state.requestBuffer())
            .map(this::processing)
            .orElse(Processing.INCOMPLETE);
    }

    private Processing processing(YellinRequest request) {
        log.debug("Processing {}", request);
        return switch (request) {
            case YellinRequest.Preflight _ -> handlePreflight(true);
            case YellinRequest.Health ignored -> handleHealth();
            case YellinRequest.Auth(ExtAuthResponse response) -> handleCurrentLease(response);
            case YellinRequest.Lease(LeasesRequest leasesRequest) -> handleNewLease(leasesRequest);
            case YellinRequest.Unknown unknown -> fail(unknown);
        };
    }

    private Processing handleCurrentLease(ExtAuthResponse extAuthResponse) {
        return leasesDispatcher.currentLease(extAuthResponse)
            .map(activation -> {
                log.debug("User {} has access to {} tracks", activation.name(), activation.size());
                writeResponse(activation);
                log.debug("Wrote back {}", activation);
                return Processing.OK;
            }).orElse(Processing.REJECTED);
    }

    private Processing handleNewLease(LeasesRequest leasesRequest) {
        return leasesDispatcher.requestLease(leasesRequest)
            .map(result -> {
                log.debug("User {} gets access to {} tracks", result.trackUUIDs().size(), result);
                writeResponse(result);
                log.debug("Wrote back {}", result);
                return Processing.OK;
            }).orElse(Processing.REJECTED);
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
        return Processing.FAIL;
    }
}
