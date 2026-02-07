package taninim.kudu.server;

import module java.base;
import com.github.kjetilv.uplift.synchttp.HttpCallbackProcessor;
import com.github.kjetilv.uplift.synchttp.HttpMethod;
import com.github.kjetilv.uplift.synchttp.req.HttpReq;
import com.github.kjetilv.uplift.synchttp.write.HttpResponseCallback;
import com.github.kjetilv.uplift.util.Maybe;
import taninim.kudu.Kudu;
import taninim.kudu.TrackRange;

import static com.github.kjetilv.uplift.synchttp.HttpMethod.*;

public final class KuduHttpHandler
    implements HttpCallbackProcessor.HttpHandler {

    private final Kudu kudu;

    public KuduHttpHandler(Kudu kudu) {
        this.kudu = Objects.requireNonNull(kudu, "kudu");
    }

    @Override
    public void handle(HttpReq httpReq, HttpResponseCallback callback) {
        switch (Maybe.a(KuduRequest.from(httpReq))) {
            case Maybe.A(KuduRequest.Audio(var track, var range, var token)) -> {
                if (Maybe.a(kudu.audioBytes(new TrackRange(track, range, token))) instanceof
                    Maybe.A(Kudu.AudioBytes(_, var chunk, var bytes))
                ) {
                    callback.status(206)
                        .headers(
                            "accept-ranges: bytes",
                            "content-range: " + chunk.rangeResponseHeader(),
                            "connection: keep-alive",
                            "cache-control: no-cache"
                        )
                        .contentType(chunk.contentType())
                        .contentLength(chunk.length())
                        .body(bytes);
                } else {
                    callback.status(404);
                }
            }
            case Maybe.A(KuduRequest.Library(var token)) -> {
                if (Maybe.a(kudu.library(token)) instanceof Maybe.A(var bytes)) {
                    callback.status(200)
                        .contentType("application/json")
                        .contentLength(bytes.length)
                        .body(bytes);
                } else {
                    callback.status(404);
                }
            }
            case Maybe.A(KuduRequest.Health()) -> callback.status(200)
                .headers("cache-control: no-cache");
            case Maybe.A(KuduRequest.Preflight()) -> callback.status(204)
                .cors(OPTIONS, GET, post(httpReq))
                .headers(
                    "access-control-max-age: 86400",
                    "vary: Accept-Encoding, Origin",
                    "cache-control: no-cache"
                );
            case Maybe.Nothing<?> _ -> callback.status(404);
        }
    }

    private static HttpMethod post(HttpReq httpReq) {
        return httpReq.isPost() ? POST : null;
    }
}
