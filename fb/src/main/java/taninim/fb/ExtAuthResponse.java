package taninim.fb;

import com.github.kjetilv.uplift.json.anno.JsonRecord;

import java.math.BigInteger;
import java.time.Duration;

@JsonRecord
public record ExtAuthResponse(
    String userID,
    String accessToken,
    String signedRequest,
    Duration expiresIn,
    BigInteger data_access_expiration_time
) {

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + userID + "]";
    }
}
