package mediaserver.externals;

import mediaserver.util.DAC;

public final class FacebookAuthResponse {

    private String userID;

    private String accessToken;

    private String signedRequest;

    private String timeoutInSeconds;

    public String getUserID() {

        return userID;
    }

    @DAC
    public void setUserID(String userID) {

        this.userID = userID;
    }

    public String getAccessToken() {

        return accessToken;
    }

    @DAC
    public void setAccessToken(String accessToken) {

        this.accessToken = accessToken;
    }

    @DAC
    public String getSignedRequest() {

        return signedRequest;
    }

    @DAC
    public void setSignedRequest(String signedRequest) {

        this.signedRequest = signedRequest;
    }

    @DAC
    public String getTimeoutInSeconds() {

        return timeoutInSeconds;
    }

    @DAC
    public void setTimeoutInSeconds(String timeoutInSeconds) {

        this.timeoutInSeconds = timeoutInSeconds;
    }
}
