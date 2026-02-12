package taninim.yellin.server;

import module java.base;
import com.github.kjetilv.uplift.json.gen.JsonRW;
import com.github.kjetilv.uplift.synchttp.HttpCallbackProcessor;
import com.github.kjetilv.uplift.synchttp.req.HttpReq;
import com.github.kjetilv.uplift.synchttp.write.HttpResponseCallback;
import com.github.kjetilv.uplift.util.Maybe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taninim.yellin.*;

import static com.github.kjetilv.uplift.synchttp.HttpMethod.*;

@SuppressWarnings("LoggingSimilarMessage")
public class YellinHttpHandler implements HttpCallbackProcessor.HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(YellinHttpHandler.class);

    private final Yellin yellin;

    public YellinHttpHandler(Yellin yellin) {
        this.yellin = Objects.requireNonNull(yellin, "leasesDispatcher");
    }

    @Override
    public void handle(HttpReq httpReq, HttpResponseCallback callback) {
        switch (Maybe.a(YellinRequest.from(httpReq))) {
            case Maybe.A(YellinRequest.Auth(var response)) -> yellin.currentLease(response)
                .ifPresentOrElse(
                    activation ->
                        respondWith(activation, callback),
                    () ->
                        callback.status(400)
                );
            case Maybe.A(YellinRequest.Lease(var leasesRequest)) -> yellin.requestLease(leasesRequest)
                .ifPresentOrElse(
                    result ->
                        respondWith(leasesRequest, result, callback),
                    () ->
                        callback.status(400)
                );
            case Maybe.A(YellinRequest.Preflight()) -> callback.status(200)
                .cors("*", GET, HEAD, POST, DELETE)
                .headers("""
                    access-control-max-age: 86400
                    vary: Accept-Encoding, Origin
                    cache-control: no-cache
                    """);
            case Maybe.A(YellinRequest.Health()) -> callback.status(200);
            case Maybe.Nothing<?> _ -> callback.status(404);
        }
    }

    protected static <T extends Record> void write(HttpResponseCallback.Headers headers, JsonRW<T> instance, T t) {
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
