function checkLoginState() {
    FB.getLoginStatus(function (response) {
            if (response.status === 'connected') {
                handleConnected(response.authResponse)
            } else {
                FB.login(function (response) {
                    if (response.status === 'connected') {
                        handleConnected(response.authResponse);
                    } else {
                        alert("Login failed!")
                    }
                });
            }
        }
    );
}

function dropLoginState() {
    FB.logout(function (response) {
    });
    postData('/auth', "");
    alert("Goodbye");
}

function openEscapeHatch() {
    document.getElementById('fb-logout').style = 'display: block';
}

async function handleConnected(authResponse) {
    let response = await postData('/auth', authResponse);
    if (response.status === 200 && response.text && response.text()) {
        Location.reload(false);
    } else {
        alert('General Error: Major Malfunction [Colonel panic]');
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
