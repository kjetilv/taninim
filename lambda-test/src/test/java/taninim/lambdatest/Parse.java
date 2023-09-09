package taninim.lambdatest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.kjetilv.uplift.json.Json;

final class Parse {

    private Parse() {

    }

    @SuppressWarnings("unchecked")
    static Lambdas2Test.LeasesActivation leasesActivation(String body) {
        Map<String, ?> map = (Map<String, ?>) Json.STRING_2_JSON_MAP.apply(body);
        return new Lambdas2Test.LeasesActivation(
            getMay(map, "name"),
            getReq(map, "userId"),
            getReq(map, "token"),
            (List<String>) map.get("trackUUIDs"),
            getOpt(map, "expiry").map(Long::parseLong).orElse(0L)
        );
    }

    @SuppressWarnings("unchecked")
    static Lambdas2Test.AuthResponse authResponse(String body) {
        Map<?, ?> map = Json.STRING_2_JSON_MAP.apply(body);
        return new Lambdas2Test.AuthResponse(
            getMay(map, "name"),
            getReq(map, "userId"),
            getReq(map, "token"),
            (List<String>) map.get("trackUUIDs"),
            getOpt(map, "expiry").map(Long::parseLong).orElse(0L)
        );
    }

    private static String getMay(Map<?, ?> map, String key) {
        return getOpt(map, key).orElse(null);
    }

    private static String getReq(Map<?, ?> map, String key) {
        return getOpt(map, key).orElseThrow(() -> new IllegalStateException("No value " + key));
    }

    private static Optional<String> getOpt(Map<?, ?> map, String key) {
        return Optional.ofNullable(map.get(key))
            .map(Object::toString);
    }
}
