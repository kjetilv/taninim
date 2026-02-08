package taninim.yellin.server;

import module java.base;
import com.github.kjetilv.uplift.synchttp.req.HttpReq;
import com.github.kjetilv.uplift.synchttp.req.QueryParameters;
import taninim.fb.ExtAuthResponse;
import taninim.fb.ExtAuthResponseRW;
import taninim.yellin.LeasesData;
import taninim.yellin.LeasesDataRW;
import taninim.yellin.LeasesRequest;
import taninim.yellin.Operation;

import static taninim.yellin.Operation.ACQUIRE;
import static taninim.yellin.Operation.RELEASE;

sealed interface YellinRequest {

    static Optional<? extends YellinRequest> from(HttpReq httpReq) {
        var requestLine = httpReq.reqLine();
        var body = httpReq.body();
        return switch (httpReq.method()) {
            case POST -> requestLine.urlPrefixed("/auth")
                ? Optional.of(new Auth(extAuth(httpReq, body)))
                : requestLine.urlPrefixed("/lease")
                  ? Optional.of(lease(httpReq, body))
                    : Optional.empty();
            case DELETE -> httpReq.queryParameters() instanceof QueryParameters qps &&
                           qps.par("userId") instanceof String userId &&
                           qps.par("token") instanceof String token &&
                           qps.par("album") instanceof String album
                ? Optional.of(lease(userId, token, album))
                : Optional.empty();
            case OPTIONS, HEAD -> Optional.of(new Preflight());
            case GET -> requestLine.urlPrefixed("/health")
                ? Optional.of(new Health())
                : Optional.empty();
            default -> Optional.empty();
        };
    }

    private static ExtAuthResponse extAuth(HttpReq httpReq, ReadableByteChannel body) {
        return ExtAuthResponseRW.INSTANCE.channelReader(httpReq.contentLength()).read(body);
    }

    private static Lease lease(String userId, String token, String album) {
        return lease(
            RELEASE,
            new LeasesData(userId, token, album)
        );
    }

    private static Lease lease(HttpReq httpReq, ReadableByteChannel body) {
        return lease(
            ACQUIRE,
            LeasesDataRW.INSTANCE.channelReader(httpReq.contentLength()).read(body)
        );
    }

    private static Lease lease(Operation release, LeasesData data) {
        return new Lease(new LeasesRequest(release, data));
    }

    record Preflight() implements YellinRequest {
    }

    record Health() implements YellinRequest {
    }

    record Lease(LeasesRequest request) implements YellinRequest {
    }

    record Auth(ExtAuthResponse response) implements YellinRequest {
    }
}
