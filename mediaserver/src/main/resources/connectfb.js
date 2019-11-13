function checkLoginState() {
    FB.getLoginStatus(function (response) {
            if (response.status === 'connected') {
                const authResponse = response.authResponse;
                const accessToken = authResponse.accessToken;
                const userID = authResponse.userID;
                const signedRequest = authResponse.signedRequest;
                const timeoutInSeconds = authResponse.expiresIn;

                document.querySelector(
                    '#fbauth input[id="userID"]').value = userID;
                document.querySelector(
                    '#fbauth input[id="accessToken"]').value = accessToken;
                document.querySelector(
                    '#fbauth input[id="signedRequest"]').value = signedRequest;
                document.querySelector(
                    '#fbauth input[id="timeoutInSeconds"]').value = timeoutInSeconds;
                document.querySelector(
                    '#fbauth').submit();

            } else {
                alert("Login failed!")
            }
        }
    );
}
