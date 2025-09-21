package taninim.yellin;

import module java.base;
import module taninim.fb;
import module taninim.taninim;
import module uplift.json;
import module uplift.lambda;
import module uplift.s3;
import com.github.kjetilv.uplift.json.JsonWriter;

import static java.util.Objects.requireNonNull;
import static taninim.yellin.Yellin.leasesDispatcher;

public final class YellinLambdaHandler extends LambdaHandlerSupport {

    public static LambdaHandler handler(
        LambdaClientSettings clientSettings,
        TaninimSettings taninimSettings,
        S3AccessorFactory s3AccessorFactory,
        FbAuthenticator fbAuthenticator
    ) {
        S3Accessor s3Accessor = s3AccessorFactory.create();
        LeasesDispatcher leasesDispatcher = leasesDispatcher(
            s3Accessor,
            clientSettings.time(),
            taninimSettings.sessionDuration(),
            taninimSettings.leaseDuration(),
            fbAuthenticator
        );
        return new YellinLambdaHandler(leasesDispatcher);
    }

    private final LeasesDispatcher leasesDispatcher;

    private YellinLambdaHandler(LeasesDispatcher leasesDispatcher) {
        this.leasesDispatcher = requireNonNull(leasesDispatcher, "ticketDispatcher");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + leasesDispatcher + "]";
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
        ExtAuthResponse extAuthResponse = RESPONSE_JSON_READER.read(body);
        return leasesDispatcher.createLease(extAuthResponse)
            .map(result ->
                lambdaResult(LEASES_WRITER.write(result)))
            .orElseGet(
                errorSupplier(UNAUTHORIZED, "Failed to authenticate: {}", this));
    }

    private LambdaResult addLease(String body) {
        LeasesRequest leasesRequest = LeasesRequest.acquire(body);
        Optional<LeasesActivation> leasesActivation = leasesDispatcher.requestLease(leasesRequest);
        return leasesActivation.map(activation ->
                lambdaResult(LEASES_WRITER.write(activation)))
            .orElseGet(
                errorSupplier(BAD_REQUEST, "Failed to add lease: {}", this));
    }

    private LambdaResult removeLease(String body) {
        LeasesRequest leasesRequest = LeasesRequest.release(body);
        return leasesDispatcher.dismissLease(leasesRequest)
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
        return Optional.ofNullable(body).map(bodyHandler);
    }
}
