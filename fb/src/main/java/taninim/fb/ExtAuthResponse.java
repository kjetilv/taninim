package taninim.fb;

import java.math.BigInteger;
import java.time.Duration;
import java.util.Map;
import java.util.function.Function;

public record ExtAuthResponse(
    String userID,
    String accessToken,
    String signedRequest,
    Duration expiresIn,
    BigInteger data_access_expiration_time
) {

    public static ExtAuthResponse from(
        String body,
        Function<? super String, ? extends Map<String, Object>> jsonParser
    ) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("No data");
        }
        try {
            Function<String, String> f = lookup(body, jsonParser).andThen(String::valueOf);
            return new ExtAuthResponse(
                f.apply("userID"),
                f.apply("accessToken"),
                f.apply("signedRequest"),
                Duration.parse("PT%sS".formatted(f.apply("expiresIn"))),
                new BigInteger(f.apply("data_access_expiration_time"))
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read auth: " + body, e);
        }
    }

    private static Function<String, Object> lookup(
        String body, Function<? super String, ? extends Map<String, Object>> jsonParser
    ) {
        Map<String, Object> map;
        try {
            map = jsonParser.apply(body);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse body: " + body, e);
        }
        if (map == null || map.isEmpty()) {
            throw new IllegalStateException("No data: `" + body.trim() + "`");
        }
        return key ->
            map.getOrDefault(key, "");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + userID + "]";
    }
}
