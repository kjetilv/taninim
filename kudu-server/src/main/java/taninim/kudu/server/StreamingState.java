package taninim.kudu.server;

import module java.base;
import module uplift.asynchttp;

import static java.util.Objects.requireNonNull;

public record StreamingState(
    ByteBuffer requestBuffer,
    Long transferred,
    Throwable error
) implements ChannelState {

    public StreamingState(ByteBuffer byteBuffer) {
        this(byteBuffer, null, null);
    }

    StreamingState withDataSupplier(Supplier<Optional<InputStream>> data) {
        return new StreamingState(requestBuffer, transferred, error);
    }

    StreamingState transferred(Long transferred) {
        return new StreamingState(
            requestBuffer,
            requireNonNull(transferred, "transferred"),
            error
        );
    }

    StreamingState error(Throwable error) {
        return new StreamingState(
            requestBuffer,
            transferred,
            requireNonNull(error, "error")
        );
    }

    boolean isOK() {
        return error == null;
    }

    private static final String CLASS_NAME = StreamingState.class.getSimpleName();

    private static final String EMPTY = CLASS_NAME + "[<none>]";

    @Override
    public String toString() {
        return transferred == null
            ? EMPTY
            : CLASS_NAME + "[transferred=" + transferred + (error == null ? "" : " error=" + error) + "]";
    }
}
