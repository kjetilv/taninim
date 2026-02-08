package taninim.yellin;

import module java.base;
import com.github.kjetilv.uplift.json.JsonReader;
import com.github.kjetilv.uplift.json.JsonWriter;
import com.github.kjetilv.uplift.lambda.LambdaHandlerSupport;
import com.github.kjetilv.uplift.lambda.LambdaPayload;
import com.github.kjetilv.uplift.lambda.LambdaResult;
import taninim.fb.ExtAuthResponse;
import taninim.fb.ExtAuthResponseRW;

import static java.util.Objects.requireNonNull;

public final class YellinLambdaHandler extends LambdaHandlerSupport {

    private final Yellin yellin;

    public YellinLambdaHandler(Yellin yellin) {
        this.yellin = requireNonNull(yellin, "ticketDispatcher");
    }

    @Override
    protected Optional<LambdaResult> lambdaResult(LambdaPayload payload) {
        if (payload.isPost()) {
            if (payload.isExactly("/auth")) {
                return handle(payload.body(), this::authenticate);
            }
            if (payload.isExactly("/lease")) {
                return handle(payload.body(), this::addLease);
            }
            if (payload.isExactly("/unlease")) {
                return handle(payload.body(), this::removeLease);
            }
        }
        if (payload.isExactly("DELETE", "/lease")) {
            return handle(payload.body(), this::removeLease);
        }
        return Optional.empty();
    }

    private LambdaResult authenticate(String body) {
        var extAuthResponse = RESPONSE_JSON_READER.read(body);
        return yellin.createLease(extAuthResponse)
            .map(result ->
                lambdaResult(LEASES_WRITER.write(result)))
            .orElseGet(
                errorSupplier(UNAUTHORIZED, "Failed to authenticate: {}", this));
    }

    private LambdaResult addLease(String body) {
        return yellin.requestLease(LeasesRequest.acquire(body))
            .map(activation ->
                lambdaResult(LEASES_WRITER.write(activation)))
            .orElseGet(
                errorSupplier(BAD_REQUEST, "Failed to add lease: {}", this));
    }

    private LambdaResult removeLease(String body) {
        return yellin.dismissLease(LeasesRequest.release(body))
            .map(result ->
                lambdaResult(LEASES_WRITER.write(result)))
            .orElseGet(
                errorSupplier(BAD_REQUEST, "Failed to remove lease: {}", this));
    }

    private static final JsonWriter<byte[], LeasesActivation, ByteArrayOutputStream> LEASES_WRITER =
        LeasesActivationRW.INSTANCE.bytesWriter();

    private static final JsonReader<String, ExtAuthResponse> RESPONSE_JSON_READER =
        ExtAuthResponseRW.INSTANCE.stringReader();

    private static Optional<LambdaResult> handle(String body, Function<String, LambdaResult> bodyHandler) {
        return Optional.ofNullable(body)
            .map(bodyHandler);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + yellin + "]";
    }
}
