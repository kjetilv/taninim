function fbLogin() {
    FB.getLoginStatus(function (statusResponse) {
        if (statusResponse.status === 'connected') {
            handleConnected(statusResponse.authResponse);
        } else {
            FB.login(function (loginResponse) {
                if (loginResponse.status === 'connected') {
                    handleConnected(loginResponse.authResponse);
                } else {
                    alert("Login failed!")
                }
            });
        }
    });
}

async function handleConnected(authResponse) {
    let response = postData('/auth', authResponse);
    if (response.status === 200) {
        if (document.getElementById('user').innerText === "stranger") {
            location.reload()
        }
    }
}

async function postData(url = '', data = {}) {
    return await fetch(url, {
        method: 'POST',
        mode: 'same-origin',
        cache: 'no-cache',
        credentials: 'same-origin',
        headers: {
            'Content-Type': 'application/json'
        },
        redirect: 'error',
        body: JSON.stringify(data)
    });
}
