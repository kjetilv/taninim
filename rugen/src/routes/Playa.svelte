<svelte:head>
    <script src="https://cdn.plyr.io/3.7.3/plyr.js"></script>
    <link rel="stylesheet" href="https://cdn.plyr.io/3.7.3/plyr.css" />
</svelte:head>

<script>
    export let album;

    export let track;

    export let authorization = false;

    export let endpoint;

    export let format = "m4a";

    $: track = false;

    $: duration = track && track.length || 0;
</script>

{#if authorization && track}
    <div class="dropdown dropdown-top" style="width: 100%">
        <label tabindex="0" class="btn m-1" style="width: 95%; alignment: center">
            {album.name} ({album.year})
        </label>
        <ul tabindex="0" class="dropdown-content menu p-2 shadow bg-base-100 rounded-box">
            <li><a href="{album.discog}" target="_blank">{album.name} ({album.year})</a>
                <br />
                {album.obi}
                <br />
                Series: {album.series.flatMap(ser => ser.name).join(", ")}
            </li>
        </ul>
    </div>
    <div class="dropdown dropdown-top" style="width: 100%">
        <label tabindex="0" class="btn m-1" style="width: 95%; alignment: center">
            {track.no}. {track.name}
        </label>
        <ul tabindex="0" class="dropdown-content menu p-2 shadow bg-base-100 rounded-box w-52">
            {#if album.sections?.length === 1}
                {#each album.sections.flatMap(section => section.tracks) as trck, i}
                    <li on:click={() => track = trck} on:keypress={() => track = trck}
                    ><a>{trck.no}. {trck.name}</a></li>
                {/each}
            {:else }
                {#each album.sections as section, i}
                    <li>{section.name}</li>
                    {#each section.tracks as trck, i}
                        <li on:click={() => track = trck} on:keypress={() => track = trck}
                        ><a>{trck.no}. {trck.name}</a></li>
                    {/each}
                {/each}
            {/if}
        </ul>
    </div>
    <div>
        <audio id="player" style="width: 100%"
               title="{track?.name || 'No track'}"
               controls
               src="{endpoint}/audio/{track.uuid}.{format}?t={authorization.token}"
               autoplay="autoplay"
               preload="metadata">
        </audio>
    </div>
{/if}
