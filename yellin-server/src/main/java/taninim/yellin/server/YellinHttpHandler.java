package taninim.yellin.server;

import module java.base;
import com.github.kjetilv.uplift.json.gen.JsonRW;
import com.github.kjetilv.uplift.synchttp.HttpHandler;
import com.github.kjetilv.uplift.synchttp.rere.HttpReq;
import com.github.kjetilv.uplift.synchttp.write.HttpResponseCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taninim.auth.Authed;
import taninim.yellin.*;

@SuppressWarnings("LoggingSimilarMessage")
public class YellinHttpHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(YellinHttpHandler.class);

    private final Yellin yellin;

    public YellinHttpHandler(Yellin yellin) {
        this.yellin = Objects.requireNonNull(yellin, "leasesDispatcher");
    }

    @Override
    public void handle(HttpReq httpReq, HttpResponseCallback callback) {
        YellinRequest.from(httpReq)
            .ifPresentOrElse(
                yellinRequest -> {
                    switch (yellinRequest) {
                        case YellinRequest.Auth(var response) -> {
                            switch (yellin.currentLease(response)) {
                                case Authed.OK(var activation) -> respondWith(activation, callback);
                                case Authed.Failed(_) -> callback.status(401);
                                case Authed.Empty<?> _ -> callback.status(404);
                            }
                        }
                        case YellinRequest.Lease(var leasesRequest) -> {
                            switch (yellin.requestLease(leasesRequest)) {
                                case Authed.OK(var result) -> respondWith(leasesRequest, result, callback);
                                case Authed.Failed(_) -> callback.status(401);
                                case Authed.Empty<?> _ -> callback.status(404);
                            }
                        }
                        case YellinRequest.Health _ -> callback.status(200);
                        case YellinRequest.Preflight _ -> callback.status(204);
                    }
                },
                () ->
                    callback.status(400)
            );
    }

    private static <T extends Record> void write(HttpResponseCallback.Headers headers, JsonRW<T> instance, T t) {
        headers.channel(out ->
            instance.chunkedChannelWriter(1024)
                .write(t, out));
    }

    private static void respondWith(
        LeasesRequest leasesRequest,
        LeasesActivation result,
        HttpResponseCallback callback
    ) {
        log.debug("User {} gets access to {} tracks", result.trackUUIDs().size(), result);
        write(
            callback.status(200)
                .contentType("application/json")
                .headers("Cache-Control: no-cache"),
            LeasesDataRW.INSTANCE,
            leasesRequest.leasesData()
        );
        log.debug("Wrote back {}", result);
    }

    private static void respondWith(
        LeasesActivation activation,
        HttpResponseCallback callback
    ) {
        log.debug("User {} has access to {} tracks", activation.name(), activation.size());
        write(
            callback.status(200)
                .contentType("application/json")
                .headers("Cache-Control: no-cache"),
            LeasesActivationRW.INSTANCE,
            activation
        );
        log.debug("Wrote back {}", activation);
    }

}
