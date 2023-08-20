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
        Function<? super String, ? extends Map<String, Object>> parser
    ) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("No data");
        }
        try {
            Function<String, String> f = lookup(body, parser).andThen(String::valueOf);
            Duration expiresInTime;
            try {
                expiresInTime = Duration.parse(f.apply(EXPIRES_IN));
            } catch (Exception e) {
                throw new IllegalStateException(
                    "Could not parse duration: " + f.apply(EXPIRES_IN), e);
            }
            BigInteger expirationTime;
            try {
                expirationTime = new BigInteger(f.apply(DATA_ACCESS_EXPIRATION_TIME));
            } catch (Exception e) {
                throw new IllegalStateException(
                    "Could not parse expiry time: " + f.apply(DATA_ACCESS_EXPIRATION_TIME), e);
            }
            return new ExtAuthResponse(
                f.apply(USER_ID),
                f.apply(ACCESS_TOKEN),
                f.apply(SIGNED_REQUEST),
                expiresInTime,
                expirationTime
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read auth: " + body, e);
        }
    }

    private static final String DATA_ACCESS_EXPIRATION_TIME = "data_access_expiration_time";

    private static final String EXPIRES_IN = "expiresIn";

    private static final String ACCESS_TOKEN = "accessToken";

    private static final String USER_ID = "userID";

    private static final String SIGNED_REQUEST = "signedRequest";

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
