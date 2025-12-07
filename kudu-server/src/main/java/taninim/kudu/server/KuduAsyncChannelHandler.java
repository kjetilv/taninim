package taninim.kudu.server;

import module java.base;
import com.github.kjetilv.uplift.asynchttp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taninim.kudu.Kudu;
import taninim.kudu.TrackRange;
import taninim.music.aural.Chunk;

import static com.github.kjetilv.uplift.asynchttp.Processing.*;
final class KuduAsyncChannelHandler extends AbstractAsyncChannelHandler<StreamingState, KuduAsyncChannelHandler> {

    private static final Logger log = LoggerFactory.getLogger(KuduAsyncChannelHandler.class);

    static AsyncChannelHandler<StreamingState, KuduAsyncChannelHandler> create(
        Kudu kudu,
        int maxRequestLength,
        int bufferSize,
        Supplier<Instant> time
    ) {
        return new KuduAsyncChannelHandler(kudu, maxRequestLength, bufferSize, time);
    }

    private final Kudu kudu;

    private final int bufferSize;

    private KuduAsyncChannelHandler(Kudu kudu, int maxRequestLength, int bufferSize, Supplier<Instant> time) {
        this(kudu, null, maxRequestLength, bufferSize, time);
    }

    private KuduAsyncChannelHandler(
        Kudu kudu,
        AsynchronousByteChannel channel,
        int maxRequestLength,
        int bufferSize,
        Supplier<Instant> time
    ) {
        super(channel, maxRequestLength, time);
        this.kudu = kudu;
        this.bufferSize = bufferSize;
    }

    @Override
    public StreamingState channelState(ByteBuffer byteBuffer) {
        return StreamingState.from(byteBuffer);
    }

    @Override
    public KuduAsyncChannelHandler bind(AsynchronousByteChannel channel) {
        return new KuduAsyncChannelHandler(kudu, channel, maxRequestLength(), bufferSize, clock());
    }

    @Override
    protected Processing process(StreamingState state) {
        var byteBuffer = state.requestBuffer();
        if (!byteBuffer.hasArray()) {
            return FAIL;
        }
        return ReceivedRequest.fromHttp(byteBuffer)
            .map(receivedRequest ->
                receivedRequest.reject() ? REJECTED
                    : receivedRequest.retry() ? INCOMPLETE
                        : receivedRequest.preflight() ? handlePreflight(false)
                            : receivedRequest.health() ? handleHealth()
                                : handleStream(state, receivedRequest))
            .orElseGet(() -> {
                log.debug("No request read from {}", byteBuffer);
                return REJECTED;
            });
    }

    private Processing handleStream(StreamingState state, ReceivedRequest receivedRequest) {
        var processing = receivedRequest.library()
            ? handleLibrary(state, receivedRequest)
            : handleAudio(state, receivedRequest);
        return processing.orElseGet(() -> {
            log.debug("Streaming rejected: {}", receivedRequest);
            return REJECTED;
        });
    }

    private Optional<Processing> handleAudio(StreamingState state, ReceivedRequest receivedRequest) {
        return receivedRequest.optionalTrackRange()
            .flatMap(trackRange ->
                stream(state, trackRange));
    }

    private Optional<Processing> stream(StreamingState state, TrackRange trackRange) {
        log.debug("Accepted request: {}", trackRange);
        return kudu.audioStream(trackRange).map(streamable ->
                transferAudio(state, streamable.chunk(), streamable.stream()))
            .map(KuduAsyncChannelHandler::complete);
    }

    private Optional<Processing> handleLibrary(StreamingState state, ReceivedRequest receivedRequest) {
        var token = receivedRequest.libraryRequest().token();
        return kudu.libraryStream(token).map(library ->
                transferLibrary(state, library))
            .map(KuduAsyncChannelHandler::complete);
    }

    private StreamingState transferLibrary(StreamingState streamingState, Kudu.Library library) {
        try (
            BufferingWriter<? super ByteBuffer> writer = responseWriter()
        ) {
            var headerBuffer = textBuffer(libraryHeaders(library.size()));
            writer.write(new WritableBuffer<>(headerBuffer, headerBuffer.capacity()));
            var bufferSize = Math.toIntExact(Math.min(this.bufferSize, library.size()));
            var buffer = ByteBuffer.allocate(bufferSize);
            BufferingReader<ByteBuffer> reader = new ByteChannelStreamBridgingReader(library.stream(), buffer);
            var written = new Transfer(library.size(), bufferSize).copy(reader, writer);
            return streamingState.transferred(written);
        } catch (Exception e) {
            return streamingState.error(e);
        }
    }

    private StreamingState transferAudio(StreamingState state, Chunk chunk, InputStream audioStream) {
        try (
            BufferingWriter<? super ByteBuffer> writer = responseWriter()
        ) {
            var headerBuffer = textBuffer(audioHeaders(chunk));
            writer.write(new WritableBuffer<>(headerBuffer, headerBuffer.capacity()));
            var size = Math.toIntExact(chunk.length());
            var bufferSize = Math.min(this.bufferSize, size);
            var buffer = ByteBuffer.allocate(bufferSize);
            BufferingReader<ByteBuffer> reader = new ByteChannelStreamBridgingReader(audioStream, buffer);
            var written = new Transfer(chunk.length(), bufferSize).copy(reader, writer);
            return state.transferred(written);
        } catch (Exception e) {
            return state.error(e);
        }
    }

    private static Processing complete(StreamingState finalState) {
        if (finalState.isOK()) {
            try {
                return OK;
            } finally {
                log.debug("Request completed: {}", finalState);
            }
        }
        try {
            return FAIL;
        } finally {
            log.error("Request failed: {}", finalState, finalState.error());
        }
    }

    private static String audioHeaders(Chunk chunk) {
        return String.join(
            "\n",
            "HTTP/1.1 206 Partial Content",
            "Content-Type: audio/" + chunk.format(),
            "Accept-Ranges: bytes",
            "Content-Range: " + chunk.rangeResponseHeader(),
            "Content-Length: " + chunk.length(),
            "Connection: keep-alive",
            "",
            ""
        );
    }

    private static String libraryHeaders(long fileSize) {
        return String.join(
            "\n",
            "HTTP/1.1 200 OK",
            "Content-Type: application/json",
            "Content-Length: " + fileSize,
            "Content-Encoding: gzip",
            "Connection: keep-alive",
            "",
            ""
        );
    }
}
