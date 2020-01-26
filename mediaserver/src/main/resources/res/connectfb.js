// noinspection JSUnusedGlobalSymbols
function fbLogin() {
    // noinspection JSUnresolvedVariable
    FB.getLoginStatus(function (statusResponse) {
        if (statusResponse.status === 'connected') {
            connected(statusResponse.authResponse)
        } else {
            // noinspection JSUnresolvedVariable,JSUnresolvedFunction
            FB.login(function (loginResponse) {
                if (loginResponse.status === 'connected') {
                    connected(loginResponse.authResponse)
                } else {
                    alert("Login failed!")
                }
            });
        }
    });
}

async function connected(authResponse) {
    postData('/auth', authResponse).then(() => window.location.href = '/')
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
