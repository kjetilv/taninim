package taninim.kudu;

import com.github.kjetilv.uplift.kernel.io.Range;
import com.github.kjetilv.uplift.uuid.Uuid;
import com.github.kjetilv.uplift.lambda.*;
import com.github.kjetilv.uplift.s3.S3AccessorFactory;
import taninim.TaninimSettings;

import java.util.Objects;
import java.util.Optional;

import static java.util.Map.entry;

public final class KuduLambdaHandler extends LambdaHandlerSupport {

    public static LambdaHandler create(
        LambdaClientSettings clientSettings,
        TaninimSettings taninimSettings,
        S3AccessorFactory s3AccessorFactory
    ) {
        return new KuduLambdaHandler(
            DefaultKudu.create(clientSettings, taninimSettings, s3AccessorFactory));
    }

    private final Kudu kudu;

    private KuduLambdaHandler(Kudu kudu) {
        this.kudu = Objects.requireNonNull(kudu, "kudu");
    }

    @Override
    protected Optional<LambdaResult> lambdaResult(LambdaPayload payload) {
        return payload.isPrefixed("get", "/audio/") ? handleAudio(payload, token(payload))
            : payload.isExactly("get", "/library.json") ? handleLibrary(token(payload))
                : Optional.empty();
    }

    private Optional<LambdaResult> handleAudio(LambdaPayload payload, Uuid token) {
        String path = payload.path("/audio/");
        Range range = payload.header("range")
            .flatMap(Range::read)
            .orElseGet(() ->
                new Range(0L, DEFAULT_START_RANGE));
        return Track.parseTrack(path)
            .map(track ->
                new TrackRange(track, range, token))
            .flatMap(kudu::audioBytes)
            .map(KuduLambdaHandler::toResult);
    }

    private Optional<LambdaResult> handleLibrary(Uuid token) {
        return kudu.library(token).map(KuduLambdaHandler::zippedJsonResult);
    }

    private static final int PARTIAL_RESULT = 206;

    private static final long DEFAULT_START_RANGE = 1_024L;

    private static Uuid token(LambdaPayload payload) {
        return Uuid.from(payload.queryParam("t"));
    }

    private static LambdaResult toResult(Kudu.AudioBytes audioBytes) {
        return LambdaResult.binary(
            PARTIAL_RESULT,
            audioBytes.bytes(),
            entry("content-type", "audio/" + audioBytes.trackRange().format()),
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
            entry("content-type", "application/json"),
            entry("content-length", String.valueOf(bytes.length)),
            entry("content-encoding", "gzip")
        );
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + kudu + "]";
    }
}
