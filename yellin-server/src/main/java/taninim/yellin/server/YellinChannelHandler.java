package taninim.yellin.server;

import com.github.kjetilv.uplift.asynchttp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taninim.fb.ExtAuthResponse;
import taninim.yellin.LeasesActivation;
import taninim.yellin.LeasesActivationRW;
import taninim.yellin.LeasesDispatcher;
import taninim.yellin.LeasesRequest;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousByteChannel;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

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
        return YellinRequest.read(state.requestBuffer())
            .map(this::processing)
            .orElse(Processing.INCOMPLETE);
    }

    private Processing processing(YellinRequest request) {
        log.debug("Processing {}", request);
        if (request.isAuthRequest()) {
            return handleCurrentLease(request.fbAuth());
        }
        if (request.isLeaseRequest()) {
            return handleNewLease(request.request());
        }
        if (request.isPreflight()) {
            return handlePreflight(request.admin(), true);
        }
        if (request.isHealth()) {
            return handleHealth(request.admin());
        }
        return fail(request);
    }

    private Processing handleCurrentLease(ExtAuthResponse extAuthResponse) {
        return leasesDispatcher.createLease(extAuthResponse)
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
        Writable<ByteBuffer> writable = response(activation);
        try (BufferedWriter<ByteBuffer> writer = responseWriter()) {
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

    private static Writable<ByteBuffer> response(LeasesActivation activation) {
        byte[] body = LeasesActivationRW.INSTANCE.write(activation).getBytes(StandardCharsets.UTF_8);
        byte[] headers = jsonHeaders(body.length);
        ByteBuffer byteBuffer = ByteBuffer.wrap(append(headers, body));
        return new WritableBuffer<>(byteBuffer, byteBuffer.capacity());
    }

    private static byte[] jsonHeaders(int length) {
        return String.format(CONTENT_HEADERS, length).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] append(byte[]... parts) {
        byte[] response = new byte[Arrays.stream(parts).mapToInt(part -> part.length).sum()];
        int offset = 0;
        for (byte[] part : parts) {
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
