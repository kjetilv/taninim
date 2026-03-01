package taninim.kudu.server;

import module java.base;
import com.github.kjetilv.uplift.synchttp.HttpHandler;
import com.github.kjetilv.uplift.synchttp.rere.HttpReq;
import com.github.kjetilv.uplift.synchttp.write.HttpResponseCallback;
import taninim.auth.Authed;
import taninim.kudu.Kudu;
import taninim.kudu.TrackRange;

public final class KuduHttpHandler
    implements HttpHandler {

    private final Kudu kudu;

    public KuduHttpHandler(Kudu kudu) {
        this.kudu = Objects.requireNonNull(kudu, "kudu");
    }

    @Override
    public void handle(HttpReq httpReq, HttpResponseCallback callback) {
        KuduRequest.from(httpReq)
            .ifPresentOrElse(
                kuduRequest -> {
                    respond(kuduRequest, callback);
                },
                () -> callback.status(400)
            );
    }

    private void respond(KuduRequest kuduRequest, HttpResponseCallback callback) {
        switch (kuduRequest) {
            case KuduRequest.Audio(var track, var range, var token) -> {
                switch (kudu.audioBytes(new TrackRange(track, range, token))) {
                    case Authed.OK(Kudu.AudioBytes(_, var chunk, var bytes)) -> callback.status(206)
                        .headers(
                            "accept-ranges: bytes",
                            "content-range: " + chunk.rangeResponseHeader(),
                            "connection: keep-alive",
                            "cache-control: no-cache"
                        )
                        .contentType(chunk.contentType())
                        .contentLength(chunk.length())
                        .body(bytes);
                    case Authed.Failed(_) -> callback.status(401);
                    default -> callback.status(404);
                }
            }
            case KuduRequest.Library(var token) -> {
                switch (kudu.library(token)) {
                    case Authed.OK(var bytes) -> callback.status(200)
                        .contentType("application/json")
                        .contentLength(bytes.length)
                        .body(bytes);
                    case Authed.Failed(_) -> callback.status(401);
                    default -> callback.status(404);
                }
            }
            case KuduRequest.Health() -> callback.status(200)
                .headers("cache-control: no-cache");
            case KuduRequest.Preflight() -> callback.status(204);
            default -> callback.status(404);
        }
    }
}
