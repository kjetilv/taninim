package taninim.yellin;

import com.github.kjetilv.uplift.lambda.*;
import com.github.kjetilv.uplift.s3.S3Accessor;
import com.github.kjetilv.uplift.s3.S3AccessorFactory;
import taninim.TaninimSettings;
import taninim.fb.Authenticator;
import taninim.fb.ExtAuthResponse;
import taninim.fb.ExtAuthResponseRW;

import java.util.Optional;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
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
    protected Optional<LambdaResult> result(LambdaPayload payload) {
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
        ExtAuthResponse extAuthResponse =
            ExtAuthResponseRW.INSTANCE.read(body);
        return leasesDispatcher.createLease(extAuthResponse)
            .map(result ->
                result(LeasesActivationRW.INSTANCE.bytes(result)))
            .orElseGet(
                errorSupplier(UNAUTHORIZED, "Failed to authenticate: {}", this));
    }

    private LambdaResult addLease(String body) {
        LeasesRequest leasesRequest = LeasesRequest.acquire(body);
        Optional<LeasesActivation> leasesActivation = leasesDispatcher.requestLease(leasesRequest);
        return leasesActivation.map(activation ->
                result(LeasesActivationRW.INSTANCE.bytes(activation)))
            .orElseGet(
                errorSupplier(BAD_REQUEST, "Failed to add lease: {}", this));
    }

    private LambdaResult removeLease(String body) {
        LeasesRequest leasesRequest = LeasesRequest.release(body);
        return leasesDispatcher.dismissLease(leasesRequest)
            .map(result ->
                result(LeasesActivationRW.INSTANCE.bytes(result)))
            .orElseGet(
                errorSupplier(BAD_REQUEST, "Failed to remove lease: {}", this));
    }

    private static Optional<LambdaResult> handle(String body, Function<String, LambdaResult> bodyHandler) {
        return Optional.ofNullable(body).map(bodyHandler);
    }
}
