<script>
    import { createEventDispatcher } from "svelte";

    const dispatch = createEventDispatcher();

    export let endpoint;

    export let receivedAlbums;

    export let playableAlbums;

    export let authorization = false;

    $: authorization;

    const trackSelected = (album, track) =>
            track && dispatch("trackSelected", {
                album: album,
                track: track
            });

    const albumDismissed = album => {
        try {
            fetch(`${endpoint}/lease?userId=${authorization.userId}&album=${album.uuid}&token=${authorization.token}`, {
                method: "DELETE",
                cache: "no-cache",
                headers: {
                    "Content-Type": "application/json"
                }
            }).then(async response => {
                if (response?.status < 300) {
                    dispatch("albumDismissed", {
                        album: album
                    });
                } else {
                    dispatch("requestedAlbum", { __error: true, error: response });
                }
            });
        } catch (e) {
            return dispatch("requestedAlbum", { __error: true, error: e });
        }
    };
</script>

{#if (playableAlbums?.length) }
    <h1>Playable albums</h1>
    <br />
    <div tabindex="0" class="collapse collapse-arrow">
        {#each playableAlbums as album, i }
            <div class="card lg:card-side bg-base-50 shadow-xl">
                <figure><img class="mask mask-hexagon" src="{album.discogImage}" alt="{album.name}" /></figure>
                <div class="card-body">
                    <h2 class="card-title">{album.artist}: {album.name} ({album.year})</h2>
                    <p>{album.artists.map(artist => artist.name).join(", ")}</p>
                    <div style="float: right">
                        <div class="dropdown dropdown-top">
                            <label tabindex="0" class="btn m-1" style="width: 95%; alignment: center">
                                Songs
                            </label>
                            <ul tabindex="0" class="dropdown-content menu p-2 shadow bg-base-100 rounded-box w-52">
                                {#if album.sections?.length === 1}
                                    {#each album.sections.flatMap(section => section.tracks) as trck, i}
                                        <li on:click={() => trackSelected(album, trck)}
                                            on:keypress={() => trackSelected(album, trck)}
                                        ><a>{trck.no}. {trck.name}</a></li>
                                    {/each}
                                {:else }
                                    {#each album.sections as section, i}
                                        <li>{section.name}</li>
                                        {#each section.tracks as trck, i}
                                            <li on:click={() => trackSelected(album, trck)}
                                                on:keypress={() => trackSelected(album, trck)}
                                            ><a>{trck.no}. {trck.name}</a></li>
                                        {/each}
                                    {/each}
                                {/if}
                            </ul>
                        </div>
                    </div>
                    <div class="card-actions justify-end">
                        <button on:click={
                            () => trackSelected(album, album.sections?.at(0)?.tracks?.at(0))
                        } class="btn btn-primary">Listen
                        </button>
                        <button on:click={
                            () => albumDismissed(album)
                        } class="btn btn-primary">Release
                        </button>
                    </div>
                </div>
            </div>
        {/each}
    </div>
{/if}
