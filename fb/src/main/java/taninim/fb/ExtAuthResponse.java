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
        Function<? super String, ? extends Map<?, ?>> parser
    ) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("No data");
        }
        try {
            Function<String, String> lookup = lookup(body, parser).andThen(String::valueOf);
            return new ExtAuthResponse(
                lookup.apply(USER_ID),
                lookup.apply(ACCESS_TOKEN),
                lookup.apply(SIGNED_REQUEST),
                duration(lookup, EXPIRES_IN),
                expiration(lookup, DATA_ACCESS_EXPIRATION_TIME)
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
        String body, Function<? super String, ? extends Map<?, ?>> jsonParser
    ) {
        Map<?, ?> map;
        try {
            map = jsonParser.apply(body);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse body: " + body, e);
        }
        if (map == null || map.isEmpty()) {
            throw new IllegalStateException("No data: `" + body.trim() + "`");
        }
        return map::get;
    }

    @SuppressWarnings("SameParameterValue")
    private static Duration duration(Function<String, String> f, String key) {
        try {
            return Duration.parse( "PT%sS".formatted(f.apply(key)));
        } catch (Exception e) {
            throw new IllegalStateException(
                "Could not parse duration: " + f.apply(key), e);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static BigInteger expiration(Function<String, String> f, String key) {
        String value = f.apply(key);
        try {
            return new BigInteger(value);
        } catch (Exception e) {
            throw new IllegalStateException("Could not parse expiry time: " + value, e);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + userID + "]";
    }
}
