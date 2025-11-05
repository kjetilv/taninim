package taninim.yellin;

import module java.base;
import com.github.kjetilv.uplift.json.JsonReader;
import com.github.kjetilv.uplift.kernel.http.QueryParams;
import com.github.kjetilv.uplift.uuid.Uuid;

import static java.util.Objects.requireNonNull;

public record LeasesRequest(Op op, LeasesData leasesData) {

    public static LeasesRequest acquire(String body) {
        return new LeasesRequest(Op.ACQUIRE, LEASES_DATA_READER.read(body));
    }

    public static LeasesRequest release(String body) {
        return new LeasesRequest(Op.RELEASE, LEASES_DATA_READER.read(body));
    }

    public static Optional<LeasesRequest> releaseQueryPars(String path) {
        var look = QueryParams.get(path);
        return look.up("userId")
            .flatMap(userId ->
                look.up("album").flatMap(Uuid::maybeFrom)
                    .flatMap(album ->
                        look.up("token").flatMap(Uuid::maybeFrom)
                            .map(token ->
                                new LeasesRequest(Op.RELEASE, new LeasesData(userId, token, album)))));
    }

    public LeasesRequest {
        requireNonNull(op, "op");
        requireNonNull(leasesData, "leasesData");
    }

    private static final JsonReader<String, LeasesData> LEASES_DATA_READER = LeasesDataRW.INSTANCE.stringReader();

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + op + " " + leasesData + "]";
    }

    public enum Op {
        ACQUIRE,
        RELEASE
    }
}
