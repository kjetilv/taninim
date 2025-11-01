package taninim.fb;

import module java.base;
import com.github.kjetilv.uplift.json.anno.JsonRecord;

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
