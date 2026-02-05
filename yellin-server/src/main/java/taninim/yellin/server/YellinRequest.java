package taninim.yellin.server;

import module java.base;
import com.github.kjetilv.uplift.synchttp.req.HttpReq;
import com.github.kjetilv.uplift.synchttp.req.QueryParameters;
import taninim.fb.ExtAuthResponse;
import taninim.fb.ExtAuthResponseRW;
import taninim.yellin.LeasesData;
import taninim.yellin.LeasesDataRW;
import taninim.yellin.LeasesRequest;

import static taninim.yellin.Operation.ACQUIRE;
import static taninim.yellin.Operation.RELEASE;

sealed interface YellinRequest {

    YellinRequest PREFLIGHT_REQ = new Preflight();

    YellinRequest HEALTH_REQ = new Health();

    static Optional<YellinRequest> from(HttpReq httpReq) {
        var requestLine = httpReq.reqLine();
        var body = httpReq.body();
        return switch (httpReq.method()) {
            case POST -> {
                var length = httpReq.contentLength();
                if (requestLine.urlPrefixed("/auth")) {
                    yield Optional.of(new Auth(
                        ExtAuthResponseRW.INSTANCE.channelReader(length).read(body)
                    ));
                }
                if (requestLine.urlPrefixed("/lease")) {
                    yield Optional.of(new Lease(
                        new LeasesRequest(
                            ACQUIRE,
                            LeasesDataRW.INSTANCE.channelReader(length).read(body)
                        )));
                }
                yield Optional.empty();
            }
            case DELETE -> {
                if (httpReq.queryParameters() instanceof QueryParameters qps &&
                    qps.par("userId") instanceof String userId &&
                    qps.par("token") instanceof String token &&
                    qps.par("album") instanceof String album) {
                    yield Optional.of(new Lease(new LeasesRequest(
                        RELEASE,
                        new LeasesData(userId, token, album)
                    )));
                }
                yield Optional.empty();
            }
            case OPTIONS, HEAD -> Optional.of(PREFLIGHT_REQ);
            case GET -> requestLine.urlPrefixed("/health")
                ? Optional.of(HEALTH_REQ)
                : Optional.empty();
            case null, default -> Optional.empty();
        };
    }

    record Preflight() implements YellinRequest {
    }

    record Health() implements YellinRequest {
    }

    record Lease(LeasesRequest request) implements YellinRequest {
    }

    record Auth(ExtAuthResponse response) implements YellinRequest {
    }

    record Unknown() implements YellinRequest {
    }
}
