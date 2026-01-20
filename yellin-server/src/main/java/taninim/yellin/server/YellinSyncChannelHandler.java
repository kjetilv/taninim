package taninim.yellin.server;

import module java.base;
import com.github.kjetilv.uplift.synchttp.HttpCallbackProcessor;
import com.github.kjetilv.uplift.synchttp.Processing;
import com.github.kjetilv.uplift.json.gen.JsonRW;
import com.github.kjetilv.uplift.synchttp.req.HttpReq;
import com.github.kjetilv.uplift.synchttp.write.HttpResponseCallback;
import com.github.kjetilv.uplift.synchttp.write.HttpResponseCallback.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taninim.fb.ExtAuthResponse;
import taninim.yellin.*;

import static com.github.kjetilv.uplift.synchttp.Processing.*;
import static com.github.kjetilv.uplift.synchttp.HttpMethod.*;

@SuppressWarnings("LoggingSimilarMessage")
class YellinSyncChannelHandler implements HttpCallbackProcessor.HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(YellinSyncChannelHandler.class);

    private final LeasesDispatcher leasesDispatcher;

    YellinSyncChannelHandler(LeasesDispatcher leasesDispatcher) {
        this.leasesDispatcher = Objects.requireNonNull(leasesDispatcher, "leasesDispatcher");
    }

    @Override
    public void handle(HttpReq httpReq, HttpResponseCallback callback) {
        YellinRequests.read(httpReq)
            .ifPresentOrElse(
                request -> {
                    var processing = processing(request, callback);
                    switch (processing) {
                        case REJECTED -> callback.status(400);
                        case FAIL -> callback.status(500);
                        default -> {
                        }
                    }
                },
                () ->
                    callback.status(500)
            );
    }

    private Processing processing(YellinRequest request, HttpResponseCallback callback) {
        try {
            log.debug("Processing {}", request);
            return switch (request) {
                case YellinRequest.Preflight _ -> {
                    handlePreflight(callback);
                    yield OK;
                }
                case YellinRequest.Health ignored -> {
                    handleHealth(callback);
                    yield OK;
                }
                case YellinRequest.Auth(var response) -> handleCurrentLease(
                    response,
                    callback
                ).orElse(REJECTED);
                case YellinRequest.Lease(var leasesRequest) -> handleNewLease(
                    leasesRequest,
                    callback
                ).orElse(REJECTED);
                case YellinRequest.Unknown unknown -> fail(unknown);
            };
        } catch (Exception e) {
            log.error("Failed to process {}", request, e);
            return FAIL;
        }
    }

    private Optional<Processing> handleCurrentLease(ExtAuthResponse extAuthResponse, HttpResponseCallback callback) {
        return leasesDispatcher.currentLease(extAuthResponse)
            .map(activation -> {
                log.debug("User {} has access to {} tracks", activation.name(), activation.size());
                var status = status(callback);
                write(status, activation, LEASES_ACTIVATION);
                log.debug("Wrote back {}", activation);
                return OK;
            });
    }

    private Optional<Processing> handleNewLease(LeasesRequest leasesRequest, HttpResponseCallback callback) {
        return leasesDispatcher.requestLease(leasesRequest)
            .map(result -> {
                log.debug("User {} gets access to {} tracks", result.trackUUIDs().size(), result);
                write(
                    callback.status(200), leasesRequest.leasesData(), LEASES_DATA
                );
                log.debug("Wrote back {}", result);
                return OK;
            });
    }

    private static final JsonRW<LeasesData> LEASES_DATA = LeasesDataRW.INSTANCE;

    private static final JsonRW<LeasesActivation> LEASES_ACTIVATION = LeasesActivationRW.INSTANCE;

    private static void handleHealth(HttpResponseCallback callback) {
        status(callback);
    }

    private static void handlePreflight(HttpResponseCallback callback) {
        callback.status(200)
            .cors("*", GET, HEAD, POST, DELETE)
            .headers("""
                access-control-max-age: 86400
                vary: Accept-Encoding, Origin
                cache-control: no-cache
                """);
    }

    private static Headers status(HttpResponseCallback callback) {
        return callback.status(200)
            .contentType("application/json")
            .headers("Cache-Control: no-cache")
            .cors("*", GET, HEAD, POST, DELETE);
    }

    private static <T extends Record> void write(Headers callback, T t, JsonRW<T> instance) {
        callback.channel(out ->
            instance.chunkedChannelWriter(1024)
                .write(t, out));
    }

    private static Processing fail(YellinRequest req) {
        log.warn("Unhandled request: {}", req);
        return FAIL;
    }
}
