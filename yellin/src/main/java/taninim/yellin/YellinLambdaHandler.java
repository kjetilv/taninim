package taninim.yellin;

import com.github.kjetilv.uplift.json.Json;
import com.github.kjetilv.uplift.lambda.*;
import com.github.kjetilv.uplift.s3.S3Accessor;
import com.github.kjetilv.uplift.s3.S3AccessorFactory;
import taninim.TaninimSettings;
import taninim.fb.Authenticator;
import taninim.fb.ExtAuthResponse;

import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static taninim.yellin.Yellin.activationSerializer;
import static taninim.yellin.Yellin.leasesDispatcher;

public final class YellinLambdaHandler extends LambdaHandlerSupport {

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
        ActivationSerializer activationSerializer = activationSerializer(s3Accessor);
        return new YellinLambdaHandler(leasesDispatcher, activationSerializer);
    }

    private final LeasesDispatcher leasesDispatcher;

    private final ActivationSerializer activationSerializer;

    private YellinLambdaHandler(LeasesDispatcher leasesDispatcher, ActivationSerializer activationSerializer) {
        this.leasesDispatcher = requireNonNull(leasesDispatcher, "ticketDispatcher");
        this.activationSerializer = requireNonNull(activationSerializer, "activationSerializer");
    }

    @Override
    protected Optional<LambdaResult> result(LambdaPayload payload) {
        String body = payload.body();
        if (payload.isExactly("post", "/auth")) {
            return handleBody(body, this::authenticate);
        }
        if (payload.isExactly("post", "/lease")) {
            return handleBody(body, this::addLease);
        }
        if (payload.isExactly("delete", "/lease")) {
            return handleBody(body, this::removeLease);
        }
        return Optional.empty();
    }

    private LambdaResult authenticate(String body) {
        ExtAuthResponse extAuthResponse =
            ExtAuthResponse.from(body, Json.STRING_2_JSON_MAP);
        return leasesDispatcher.createLease(extAuthResponse)
            .map(result ->
                result(activationSerializer.jsonBody(result)))
            .orElseGet(
                errorSupplier(UNAUTHORIZED, "Failed to authenticate: {}", this));
    }

    private LambdaResult addLease(String body) {
        LeasesRequest leasesRequest = LeasesRequest.acquire(body, Json.STRING_2_JSON_MAP);
        Optional<LeasesActivation> leasesActivation = leasesDispatcher.requestLease(leasesRequest);
        return leasesActivation.map(activation ->
                result(activationSerializer.jsonBody(activation)))
            .orElseGet(
                errorSupplier(BAD_REQUEST, "Failed to add lease: {}", this));
    }

    private LambdaResult removeLease(String body) {
        LeasesRequest leasesRequest =
            LeasesRequest.release(body, Json.STRING_2_JSON_MAP);
        return leasesDispatcher.dismissLease(leasesRequest)
            .map(result ->
                result(activationSerializer.jsonBody(result)))
            .orElseGet(
                errorSupplier(BAD_REQUEST, "Failed to remove lease: {}", this));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + leasesDispatcher + ", " + activationSerializer + "]";
    }
}
