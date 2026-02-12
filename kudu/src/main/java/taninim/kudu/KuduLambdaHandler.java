package taninim.kudu;

import module java.base;
import com.github.kjetilv.uplift.hash.Hash;
import com.github.kjetilv.uplift.kernel.io.Range;
import com.github.kjetilv.uplift.lambda.LambdaHandlerSupport;
import com.github.kjetilv.uplift.lambda.LambdaPayload;
import com.github.kjetilv.uplift.lambda.LambdaResult;

import static com.github.kjetilv.uplift.hash.HashKind.K128;
import static java.util.Map.entry;

public final class KuduLambdaHandler extends LambdaHandlerSupport {

    private final Kudu kudu;

    public KuduLambdaHandler(Kudu kudu) {
        this.kudu = Objects.requireNonNull(kudu, "kudu");
    }

    @Override
    protected Optional<LambdaResult> lambdaResult(LambdaPayload payload) {
        return payload.isPrefixed("get", "/audio/") ? handleAudio(payload, token(payload))
            : payload.isExactly("get", "/library.jsonl") ? handleLibrary(token(payload))
                : Optional.empty();
    }

    private Optional<LambdaResult> handleAudio(LambdaPayload payload, Hash<K128> token) {
        var path = payload.path("/audio/");
        return Track.parse(path)
            .map(track -> {
                var range = payload.header("range")
                    .flatMap(Range::read)
                    .orElseGet(() ->
                        new Range(0L, DEFAULT_START_RANGE));
                return new TrackRange(track, range, token);
            })
            .flatMap(kudu::audioBytes)
            .map(KuduLambdaHandler::toResult);
    }

    private Optional<LambdaResult> handleLibrary(Hash<K128> token) {
        return kudu.library(token)
            .map(KuduLambdaHandler::zippedJsonResult);
    }

    private static final int PARTIAL_RESULT = 206;

    private static final long DEFAULT_START_RANGE = 1_024L;

    private static Hash<K128> token(LambdaPayload payload) {
        return Hash.from(payload.queryParam("t"));
    }

    private static LambdaResult toResult(Kudu.AudioBytes audioBytes) {
        return LambdaResult.binary(
            PARTIAL_RESULT,
            audioBytes.bytes(),
            entry("content-type", "audio/" + audioBytes.trackRange().track().format().suffix()),
            entry("accept-ranges", "bytes"),
            entry("content-range", audioBytes.chunk().rangeResponseHeader()),
            entry("content-length", String.valueOf(audioBytes.chunk().length())),
            entry("connection", "keep-alive")
        );
    }

    private static LambdaResult zippedJsonResult(byte[] bytes) {
        return LambdaResult.binary(
            OK,
            bytes,
            entry("content-type", "application/jsonlines"),
            entry("content-length", String.valueOf(bytes.length)),
            entry("content-encoding", "gzip")
        );
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + kudu + "]";
    }
}
