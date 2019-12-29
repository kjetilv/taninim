// noinspection JSUnusedGlobalSymbols
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
    postData('/auth', authResponse).then(res => {
        if (res.status === 200) {
            window.location.href = '/'
        }
    })
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
