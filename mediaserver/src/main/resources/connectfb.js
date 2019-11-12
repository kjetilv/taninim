function checkLoginState() {
    FB.getLoginStatus(function (response) {
            if (response.status === 'connected') {
                const authResponse = response.authResponse;
                const accessToken = authResponse.accessToken;
                const userID = authResponse.userID;
                const signedRequest = authResponse.signedRequest;
                const timeoutInSeconds = authResponse.expiresIn;

                document.querySelector(
                    '#fbauth input[name="userID"]').value = userID;
                document.querySelector(
                    '#fbauth input[name="accessToken"]').value = accessToken;
                document.querySelector(
                    '#fbauth input[name="signedRequest"]').value = signedRequest;
                document.querySelector(
                    '#fbauth input[name="timeoutInSeconds"]').value = timeoutInSeconds;
                document.querySelector(
                    '#fbauth').submit();

            } else {
                alert("Login failed!")
            }
        }
    );
}
