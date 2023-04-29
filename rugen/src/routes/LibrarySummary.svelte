<script>
    export let receivedAlbums;
    $: receivedAlbums;
    export let playableAlbums;
    $: playableAlbums;
</script>

<span>{playableAlbums?.length || 0}/{receivedAlbums.length}</span>
