<script>
    import { createEventDispatcher } from "svelte";

    const dispatch = createEventDispatcher();

    export let endpoint;

    export let receivedAlbums;

    export let authorization = false;

    $: authorization;

    const sections = (album) =>
            album.sections || [];

    const section = (album) =>
            album.sections[0];

    const tracks = (section) =>
            section.tracks || [];

    const time = (track) =>
            Math.floor(track.seconds / 60) + ":" + (track.seconds % 60);

    const printArtist = (album, len) =>
            album.artist.length > len
                    ? album.artist.substring(0, Math.min(len - 1, album.artist.length)) + "⋯"
                    : album.artist;

    const requestAlbum = (album) => {
        try {
            fetch(`${endpoint}/lease`, {
                method: "POST",
                cache: "no-cache",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    userId: authorization.userId,
                    token: authorization.token,
                    album: album.uuid
                })
            }).then(async response => {
                if (response?.status < 300) {
                    dispatch("rentedAlbum", { album: album });
                } else {
                    dispatch("requestedAlbum", { __error: true, error: response });
                }
            });
        } catch (e) {
            return dispatch("requestedAlbum", { __error: true, error: e });
        }
    };

</script>

<div class="overflow-x-auto">
    {#if receivedAlbums?.length}
        <h1>Library</h1>
        <br />
        <table class="table table-compact w-full">
            <thead>
            <tr>
                <th>Album</th>
                <th>Artist</th>
                <th>Year</th>
                <th></th>
            </tr>
            </thead>
            <tbody>
            {#each receivedAlbums as album, i }
                <tr>
                    <td>
                        <div tabindex="0"
                             class="collapse collapse-arrow border border-base-100 bg-base-100 rounded-box">
                            <div class="collapse-title font-small">
                                {album.name}
                            </div>
                            <div class="collapse-content">
                                <ul>
                                    {#if (sections(album).length === 1)}
                                        {#each tracks(section(album)) as track, t}
                                            <li>&nbsp;&nbsp;<span class="font-thin">{track.no}.</span>
                                                <span class="font-bold">{track.name}</span>
                                                <span class="font-thin">{time(track)}</span></li>
                                        {/each}
                                    {:else }
                                        {#each sections(album) as section, j}
                                            <li><span
                                                    class="font-thin">{section.name?.trim() || ("Disc " + (j + 1))}</span>
                                            </li>
                                            {#each tracks(section) as track, t}
                                                <li>&nbsp;&nbsp;<span class="font-thin">{track.no}.</span>
                                                    <span class="font-bold">{track.name}</span>
                                                    <span class="font-thin">{time(track)}</span></li>
                                            {/each}
                                        {/each}
                                    {/if}
                                </ul>
                            </div>
                        </div>
                    </td>
                    <td>{printArtist(album, 30)}</td>
                    <td style="align-items: end">{album.year || ""}</td>
                    <td>
                        <button class="btn w-10 rounded" on:click={requestAlbum(album)}>R</button>
                    </td>
                </tr>
            {/each}
            </tbody>
            <tfoot>
            <tr>
                <th>Album</th>
                <th>Artist</th>
                <th>Year</th>
            </tr>
            </tfoot>
        </table>
    {/if}
</div>

<!--                <th>-->
<!--                    <div class="dropdown dropdown-right dropdown-end">-->
<!--                        <label tabindex="0" class="btn m-1">⨁</label>-->
<!--                        <ul tabindex="0" class="dropdown-content menu p-2 shadow bg-base-100 rounded-box w-52">-->
<!--                            {#if (sections(album).length === 1)}-->
<!--                                {#each tracks(section(album)) as track, t}-->
<!--                                    <li>{track.no}. {track.name} ({time(track)})</li>-->
<!--                                {/each}-->
<!--                            {:else }-->
<!--                                {#each sections(album) as section, j}-->
<!--                                    <li>{section.name}</li>-->
<!--                                    {#each tracks(section) as track, t}-->
<!--                                        <li>&nbsp;&nbsp;{track.no}. {track.name} ({time(track)})</li>-->
<!--                                    {/each}-->
<!--                                {/each}-->
<!--                            {/if}-->
<!--                        </ul>-->
<!--                    </div>-->
<!--                </th>-->
