package taninim.yellin;

import module java.base;
import com.github.kjetilv.uplift.hash.Hash;
import com.github.kjetilv.uplift.hash.HashKind.K128;
import com.github.kjetilv.uplift.json.JsonReader;
import com.github.kjetilv.uplift.kernel.http.QueryParams;

import static java.util.Objects.requireNonNull;
import static taninim.yellin.Operation.*;

public record LeasesRequest(Operation operation, LeasesData leasesData) {

    public static LeasesRequest acquire(String body) {
        return new LeasesRequest(ACQUIRE, LEASES_DATA_READER.read(body));
    }

    public static LeasesRequest release(String body) {
        return new LeasesRequest(RELEASE, LEASES_DATA_READER.read(body));
    }

    public static Optional<LeasesRequest> releaseQueryPars(String path) {
        var look = QueryParams.get(path);
        return look.up("userId")
            .flatMap(userId ->
                look.up("album")
                    .map(Hash::<K128>from)
                    .flatMap(album ->
                        look.up("token")
                            .map(Hash::<K128>from)
                            .map(token ->
                                new LeasesRequest(
                                    RELEASE,
                                    new LeasesData(
                                        userId,
                                        token.digest(),
                                        album.digest()
                                    )
                                ))));
    }

    public LeasesRequest {
        requireNonNull(operation, "op");
        requireNonNull(leasesData, "leasesData");
    }

    private static final JsonReader<String, LeasesData> LEASES_DATA_READER = LeasesDataRW.INSTANCE.stringReader();

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + operation + " " + leasesData + "]";
    }
}
