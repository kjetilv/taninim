<div class="navbar bg-base-100">
    <div class="flex-1">
        <a class="btn btn-ghost normal-case text-xl" href="#">taninim</a>
    </div>
    <div class="flex-none">
        <ul class="menu menu-horizontal px-1">
            <li>
                <LibrarySummary
                        bind:playableAlbums={playableAlbums}
                        bind:receivedAlbums={receivedAlbums} />
            </li>
            <li>
                <TaninimLogin
                        endpoint={yellUrl}
                        bind:authorization={authorization}
                        on:taninimAuthorization={authorized}
                        on:taninimUnauthorized={unauthorized}
                        on:taninimUnavailable={unavailable}
                        on:taninimNotConnected={notConnected}
                />
            </li>
        </ul>
    </div>
</div>

{#if errorMessage}
    <div class="alert alert-error shadow-lg">
        <div>
            <svg xmlns="http://www.w3.org/2000/svg" class="stroke-current flex-shrink-0 h-6 w-6" fill="none"
                 viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>{errorMessage}</span>
        </div>
    </div>
{/if}

<LibraryLoad
        endpoint={kuduUrl}
        bind:authorization={authorization}
        on:receivedAlbum={receivedAlbum}
/>

<LibraryView
        endpoint={yellUrl}
        bind:authorization={authorization}
        bind:playableAlbums={playableAlbums}
        bind:receivedAlbums={receivedAlbums}
        on:trackSelected={trackSelected}
        on:albumDismissed={albumDismissed}
/>

<LibraryList
        endpoint={yellUrl}
        bind:authorization={authorization}
        bind:receivedAlbums={receivedAlbums}
        on:trackSelected={trackSelected}
        on:rentedAlbum={rentedAlbum}
        on:requestedAlbum={receivedAlbum}
/>

<div>
    <br />
    <br />
    <br />
</div>

<div class="btm-nav">
    <Playa endpoint={kuduUrl}
           bind:authorization={authorization}
           bind:album={selectedAlbum}
           bind:track={selectedTrack} />
</div>

<script>
    import LibraryLoad from "./LibraryLoad.svelte";
    import LibraryView from "./LibraryView.svelte";
    import LibraryList from "./LibraryList.svelte";
    import LibrarySummary from "./LibrarySummary.svelte";
    import TaninimLogin from "./TaninimLogin.svelte";
    import Playa from "./Playa.svelte";

    // const kuduUrl = "http://localhost:8080";
    // const yellUrl = "http://localhost:8081";
    //
    // const kuduUrl = "https://hdhk322i6jbamxue3l7npdxwem0nybdl.lambda-url.eu-north-1.on.aws";
    // const yellUrl = "https://36ndlyu7vkc7xl3hfbjjrkzrxy0uneqt.lambda-url.eu-north-1.on.aws";
    //
    const kuduUrl = "https://uymlm3tk5qfjvwbu73n3dd5sq40ssduo.lambda-url.eu-north-1.on.aws";
    const yellUrl = "https://xy7nctvhyccdaxbhyrwo57leu40nyfly.lambda-url.eu-north-1.on.aws";

    const isAuthorized = track =>
      track && track.uuid && authorization.trackUUIDs && authorization.trackUUIDs.includes(track.uuid);

    const canPlay = album =>
      authorization && album.sections?.flatMap(section => section.tracks || [])
        ?.filter(isAuthorized)
        ?.length > 0;

    let authorization = false;
    let errorMessage = "";

    let receivedAlbums = [];
    let playableAlbums;
    $: playableAlbums = receivedAlbums.filter(canPlay);

    let selectedTrack = false;
    let selectedAlbum = false;

    const receivedAlbum = event => {
        let data = event.detail;
        if (data.__done) {
            receivedAlbums = receivedAlbums;
            errorMessage = "";
        } else if (data.__clear) {
            receivedAlbums = [];
            errorMessage = "";
        } else if (data.__error) {
            console.log("Failed to load data");
            console.log(data.error);
            errorMessage = "Failed to load data";
        } else {
            receivedAlbums = [...receivedAlbums, data];
            errorMessage = "";
        }
    };

    const rentedAlbum = () => {
        authorization = authorization;
    };

    const trackSelected = ({detail}) => {
        selectedAlbum = detail.album;
        selectedTrack = detail.track;
    };

    const albumDismissed = () => {
        authorization = authorization;
    };

    const authorized = ({detail}) => {
        authorization = detail;
        errorMessage = "";
    };

    const unauthorized = ({detail}) => {
        authorization = false;
        console.log("Not auhorized");
        console.log(detail);
        errorMessage = "Not authorized";
    };

    const unavailable = ({detail}) => {
        authorization = false;
        console.log("Connection failed");
        console.log(detail);
        errorMessage = "Connection to servers failed";
    };

    const notConnected = ({detail}) => {
        authorization = false;
        console.log("Not connected");
        console.log(detail);
        errorMessage = "Not connected";
    };
</script>
