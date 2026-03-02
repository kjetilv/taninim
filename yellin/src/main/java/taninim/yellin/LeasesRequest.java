package taninim.yellin;

import module java.base;
import com.github.kjetilv.uplift.json.JsonReader;

import static java.util.Objects.requireNonNull;
import static taninim.yellin.Operation.*;

public record LeasesRequest(Operation operation, LeasesData leasesData) {

    public static LeasesRequest acquire(String body) {
        return new LeasesRequest(ACQUIRE, LEASES_DATA_READER.read(body));
    }

    public static LeasesRequest release(String body) {
        return new LeasesRequest(RELEASE, LEASES_DATA_READER.read(body));
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
