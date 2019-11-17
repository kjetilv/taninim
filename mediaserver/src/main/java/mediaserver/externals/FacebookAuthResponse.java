package mediaserver.externals;

@SuppressWarnings("unused")
public class FacebookAuthResponse {

    private String userID;

    private String accessToken;

    private String signedRequest;

    private String timeoutInSeconds;

    public String getUserID() {

        return userID;
    }

    public void setUserID(String userID) {

        this.userID = userID;
    }

    public String getAccessToken() {

        return accessToken;
    }

    public void setAccessToken(String accessToken) {

        this.accessToken = accessToken;
    }

    public String getSignedRequest() {

        return signedRequest;
    }

    public void setSignedRequest(String signedRequest) {

        this.signedRequest = signedRequest;
    }

    public String getTimeoutInSeconds() {

        return timeoutInSeconds;
    }

    public void setTimeoutInSeconds(String timeoutInSeconds) {

        this.timeoutInSeconds = timeoutInSeconds;
    }
}
