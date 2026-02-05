package taninim.yellin.server;

import module java.base;
import com.github.kjetilv.uplift.json.gen.JsonRW;
import com.github.kjetilv.uplift.synchttp.HttpCallbackProcessor;
import com.github.kjetilv.uplift.synchttp.Processing;
import com.github.kjetilv.uplift.synchttp.req.HttpReq;
import com.github.kjetilv.uplift.synchttp.write.HttpResponseCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taninim.fb.ExtAuthResponse;
import taninim.yellin.LeasesActivationRW;
import taninim.yellin.LeasesDataRW;
import taninim.yellin.LeasesDispatcher;
import taninim.yellin.LeasesRequest;

import static com.github.kjetilv.uplift.synchttp.HttpMethod.*;
import static com.github.kjetilv.uplift.synchttp.Processing.*;

@SuppressWarnings("LoggingSimilarMessage")
public class YellinHttpHandler implements HttpCallbackProcessor.HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(YellinHttpHandler.class);

    private final LeasesDispatcher leasesDispatcher;

    public YellinHttpHandler(LeasesDispatcher leasesDispatcher) {
        this.leasesDispatcher = Objects.requireNonNull(leasesDispatcher, "leasesDispatcher");
    }

    @Override
    public void handle(HttpReq httpReq, HttpResponseCallback callback) {
        YellinRequest.from(httpReq)
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
                    callback.status(200)
                        .cors("*", GET, HEAD, POST, DELETE)
                        .headers("""
                            access-control-max-age: 86400
                            vary: Accept-Encoding, Origin
                            cache-control: no-cache
                            """);
                    yield OK;
                }
                case YellinRequest.Health ignored -> {
                    callback.status(200);
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
                case YellinRequest.Unknown unknown -> {
                    log.warn("Unhandled request: {}", unknown);
                    yield Processing.FAIL;
                }
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
                write(
                    json200(callback),
                    LeasesActivationRW.INSTANCE,
                    activation
                );
                log.debug("Wrote back {}", activation);
                return OK;
            });
    }

    private Optional<Processing> handleNewLease(LeasesRequest leasesRequest, HttpResponseCallback callback) {
        return leasesDispatcher.requestLease(leasesRequest)
            .map(result -> {
                log.debug("User {} gets access to {} tracks", result.trackUUIDs().size(), result);
                write(
                    json200(callback),
                    LeasesDataRW.INSTANCE,
                    leasesRequest.leasesData()
                );
                log.debug("Wrote back {}", result);
                return OK;
            });
    }

    protected static void handleHealth(HttpResponseCallback callback) {
        callback.status(200);
    }

    protected static <T extends Record> void write(HttpResponseCallback.Headers headers, JsonRW<T> instance, T t) {
        headers.channel(out ->
            instance.chunkedChannelWriter(1024)
                .write(t, out));
    }

    private static HttpResponseCallback.Headers json200(HttpResponseCallback callback) {
        return callback.status(200)
            .contentType("application/json")
            .headers("Cache-Control: no-cache");
    }
}
