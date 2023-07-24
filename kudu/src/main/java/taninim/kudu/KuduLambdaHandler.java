package taninim.kudu;

import java.util.Optional;

import com.github.kjetilv.uplift.kernel.io.Range;
import com.github.kjetilv.uplift.kernel.uuid.Uuid;
import com.github.kjetilv.uplift.lambda.LambdaClientSettings;
import com.github.kjetilv.uplift.lambda.LambdaHandler;
import com.github.kjetilv.uplift.lambda.LambdaPayload;
import com.github.kjetilv.uplift.lambda.LambdaResult;
import com.github.kjetilv.uplift.s3.S3AccessorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taninim.TaninimSettings;

import static java.util.Map.entry;

public final class KuduLambdaHandler implements LambdaHandler {

    private static final Logger log = LoggerFactory.getLogger(KuduLambdaHandler.class);

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
        this.kudu = kudu;
    }

    @Override
    public LambdaResult handle(LambdaPayload lambdaPayload) {
        return result(lambdaPayload).orElseGet(() -> {
            log.error("No result: {} {}", lambdaPayload, BAD_REQUEST);
            return LambdaResult.status(BAD_REQUEST);
        });
    }

    private Optional<LambdaResult> result(LambdaPayload payload) {
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
        return Track.parseTrack(path).map(track ->
                new TrackRange(track, range, token))
            .flatMap(kudu::audioBytes)
            .map(KuduLambdaHandler::toResult);
    }

    private Optional<LambdaResult> handleLibrary(Uuid token) {
        return kudu.library(token).map(KuduLambdaHandler::zippedJsonResult);
    }

    private static final int BAD_REQUEST = 400;

    private static final int PARTIAL_RESULT = 206;

    private static final int OK = 200;

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
