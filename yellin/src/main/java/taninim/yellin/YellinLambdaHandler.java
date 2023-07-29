package taninim.yellin;

import java.util.function.Supplier;

import com.github.kjetilv.uplift.json.Json;
import com.github.kjetilv.uplift.lambda.LambdaClientSettings;
import com.github.kjetilv.uplift.lambda.LambdaHandler;
import com.github.kjetilv.uplift.lambda.LambdaPayload;
import com.github.kjetilv.uplift.lambda.LambdaResult;
import com.github.kjetilv.uplift.s3.S3Accessor;
import com.github.kjetilv.uplift.s3.S3AccessorFactory;
import org.slf4j.Logger;
import taninim.fb.Authenticator;
import taninim.fb.ExtAuthResponse;
import taninim.TaninimSettings;

import static com.github.kjetilv.uplift.lambda.LambdaResult.status;
import static java.util.Map.entry;
import static java.util.Objects.requireNonNull;
import static taninim.yellin.Yellin.activationSerializer;
import static taninim.yellin.Yellin.leasesDispatcher;

public final class YellinLambdaHandler implements LambdaHandler {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(YellinLambdaHandler.class);

    public static LambdaHandler handler(
        LambdaClientSettings clientSettings,
        TaninimSettings taninimSettings,
        S3AccessorFactory s3AccessorFactory,
        Authenticator authenticator
    ) {
        S3Accessor s3Accessor = s3AccessorFactory.create();
        LeasesDispatcher leasesDispatcher = leasesDispatcher(
            s3Accessor,
            clientSettings.time(),
            taninimSettings.sessionDuration(),
            taninimSettings.leaseDuration(),
            authenticator
        );
        ActivationSerializer activationSerializer =
            activationSerializer(s3Accessor);
        return new YellinLambdaHandler(
            leasesDispatcher,
            activationSerializer
        );
    }

    private final LeasesDispatcher leasesDispatcher;

    private final ActivationSerializer activationSerializer;

    private YellinLambdaHandler(
        LeasesDispatcher leasesDispatcher,
        ActivationSerializer activationSerializer
    ) {
        this.leasesDispatcher = requireNonNull(leasesDispatcher, "ticketDispatcher");
        this.activationSerializer = requireNonNull(activationSerializer, "activationSerializer");
    }

    @Override
    public LambdaResult handle(LambdaPayload lambdaPayload) {
        String body = lambdaPayload.body();

        if (body == null) {
            return error("No body in request: {}", BAD_REQUEST);
        }

        if (lambdaPayload.isExactly("post", "/auth")) {
            return authenticate(body);
        }

        if (lambdaPayload.isExactly("post", "/lease")) {
            return addLease(body);
        }

        if (lambdaPayload.isExactly("delete", "/lease")) {
            return removeLease(body);
        }

        return error("No such handler: {}", NOT_FOUND);
    }

    private LambdaResult authenticate(String body) {
        ExtAuthResponse extAuthResponse =
            ExtAuthResponse.from(body, Json.STRING_2_JSON_MAP);
        return leasesDispatcher.createLease(extAuthResponse)
            .map(result ->
                result(activationSerializer.jsonBody(result.leasesActivation())))
            .orElseGet(
                errorSupplier("Failed to authenticate: {}", UNAUTHORIZED));
    }

    private LambdaResult addLease(String body) {
        LeasesRequest leasesRequest =
            LeasesRequest.acquire(body, Json.STRING_2_JSON_MAP);
        return leasesDispatcher.requestLease(leasesRequest)
            .map(result ->
                result(activationSerializer.jsonBody(result.leasesActivation())))
            .orElseGet(
                errorSupplier("Failed to add lease: {}", BAD_REQUEST));
    }

    private LambdaResult removeLease(String body) {
        LeasesRequest leasesRequest =
            LeasesRequest.release(body, Json.STRING_2_JSON_MAP);
        return leasesDispatcher.dismissLease(leasesRequest)
            .map(result ->
                result(activationSerializer.jsonBody(result.leasesActivation())))
            .orElseGet(
                errorSupplier("Failed to remove lease: {}", BAD_REQUEST));
    }

    private static final int OK = 200;

    private static final int BAD_REQUEST = 400;

    private static final int NOT_FOUND = 404;

    private static final int UNAUTHORIZED = 401;

    private static Supplier<LambdaResult> errorSupplier(String error, int statusCode) {
        return () ->
            error(error, statusCode);
    }

    private static LambdaResult error(String error, int statusCode) {
        log.error(error, statusCode);
        return status(statusCode);
    }

    private static LambdaResult result(byte[] body) {
        return LambdaResult.json(
            OK,
            body,
            entry("Content-Type", "application/json")
        );
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + leasesDispatcher + ", " + activationSerializer + "]";
    }
}
