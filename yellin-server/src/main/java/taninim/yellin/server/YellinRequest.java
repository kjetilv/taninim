package taninim.yellin.server;

import module java.base;
import com.github.kjetilv.uplift.synchttp.rere.HttpReq;
import com.github.kjetilv.uplift.synchttp.rere.QueryParameters;
import taninim.fb.ExtAuthResponse;
import taninim.fb.ExtAuthResponseRW;
import taninim.yellin.LeasesData;
import taninim.yellin.LeasesDataRW;
import taninim.yellin.LeasesRequest;

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
                  ? Optional.of(acquire(httpReq, body))
                    : Optional.empty();
            case DELETE -> httpReq.queryParameters() instanceof QueryParameters qps &&
                           qps.par("userId") instanceof String userId &&
                           qps.par("token") instanceof String token &&
                           qps.par("album") instanceof String album
                ? Optional.of(release(userId, token, album))
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

    private static Lease release(String userId, String token, String album) {
        var leasesData = new LeasesData(userId, token, album);
        return new Lease(new LeasesRequest(RELEASE, leasesData));
    }

    private static Lease acquire(HttpReq httpReq, ReadableByteChannel body) {
        var leasesData = LeasesDataRW.INSTANCE.channelReader(httpReq.contentLength())
            .read(body);
        return new Lease(new LeasesRequest(ACQUIRE, leasesData));
    }

    record Preflight() implements YellinRequest {
    }

    record Health() implements YellinRequest {
    }

    record Lease(LeasesRequest request) implements YellinRequest {

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + request + "]";
        }
    }

    record Auth(ExtAuthResponse response) implements YellinRequest {

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + response + "]";
        }
    }
}
