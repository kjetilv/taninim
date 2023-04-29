<svelte:head>
    <script async
            defer
            crossorigin="anonymous"
            src="https://connect.facebook.net/nn_NO/sdk.js#xfbml=1&version=v15.0&appId=398404340829160&autoLogAppEvents=1"
            nonce="fkQG4DTw"
            id="fb-loader">
    </script>
</svelte:head>

<div>
    <span class="spn" on:click={fbLogin}>{authorization?.name || "Connect"}</span>
    {#if authorization}
        <button class="btn w-16 rounded" on:click={fbLoginCheck}>Reload</button>
    {/if}
</div>

<!--suppress JSUnresolvedVariable, JSUnresolvedFunction -->
<script>
    import { createEventDispatcher, onMount } from "svelte";

    export let endpoint;

    const dispatch = createEventDispatcher();

    let connectedStatus = "";

    let fbStatus = {};

    let fbLoginResult = {};

    export let authorization = false;

    $: needsLogin = connectedStatus !== "" && connectedStatus !== "connected";

    const postConnected = (data, okCallback, failedCallback, errorCallback) => {
        // noinspection AssignmentResultUsedJS
        fetch(`${endpoint}/auth`, {
            method: "POST",
            cache: "no-cache",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(data)
        }).then(async response => {
            const status = response.status;
            const headers = response.headers;
            const json = headers.get("content-type")?.includes("application/json");
            const data = json && await response.json();
            if (response.ok && json) {
                okCallback(status, data);
            } else {
                // get error message from body or default to response status
                failedCallback(status, (data && data.message) || response.status);
            }
        }).catch(error => {
            errorCallback(error);
        });
    };

    const fbHandler = (res) => {
        if (res && res.status) {
            connectedStatus = res.status;
            if (connectedStatus === "connected" && res.authResponse) {
                postConnected(
                        res.authResponse,
                        (status, data) =>
                                dispatch("taninimAuthorization", data),
                        (status, data) =>
                                dispatch("taninimUnauthorized", data),
                        (error) =>
                                dispatch("taninimUnavailable", error));
            } else {
                dispatch("taninimNotConnected", res);
            }
        } else {
            dispatch("taninimNotConnected", res);
        }
    };

    const fbLoginCheck = () =>
            // eslint-disable-next-line
            FB.getLoginStatus(fbHandler);

    const doFbLogin = () =>
            // eslint-disable-next-line
            FB.login(fbHandler, { scope: "public_profile, email" });

    const fbLogin = () => {
        if (authorization) {
            fbLoginCheck();
        } else {
            doFbLogin();
        }
    };

    onMount(fbLoginCheck);

</script>
