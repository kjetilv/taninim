package taninim.fb;

import module java.base;
import module uplift.json.anno;

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
