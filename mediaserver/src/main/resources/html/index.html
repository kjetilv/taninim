<!doctype html>
<!--suppress HtmlUnknownTarget -->
<HTML lang="no">
<HEAD>
    <LINK HREF="/res/taninim.css" REL="stylesheet">
    {if(highlighted)}
    {if(plyr)}
    <LINK HREF="/res/plyr.css" REL="stylesheet">
    {endif}
    {endif}
    <META charset="UTF-8">
    <TITLE>
        taninim[{if(artist)}{artist.target.name}{else}{if(playlist)}{playlist.name}{endif}{endif}]</TITLE>
</HEAD>

<BODY>

<DIV STYLE="float: right">
    <svg height="130pt"
         viewBox="0 0 150 150" width="120pt" xmlns="http://www.w3.org/2000/svg">
        <g fill="#708070" transform="translate(-20,175) scale(0.10,-0.10)">
            <path d="M889 1595 l-112 -195 -228 0 c-126 0 -229 -3 -229 -6 0 -4 49 -93
    110 -199 61 -106 110 -195 110 -198 0 -3 -51 -93 -113 -200 l-114 -194 81 -7
    c45 -3 127 -6 182 -6 l101 0 259 437 c143 240 258 442 256 447 -8 24 -177 316
    -183 316 -4 0 -58 -88 -120 -195z"/>
            <path d="M1074 963 c-143 -240 -258 -442 -256 -447 8 -24 177 -316 183 -316 4
    0 58 88 120 195 l112 195 228 0 c126 0 229 3 229 6 0 4 -49 93 -110 199 -60
    106 -110 195 -110 198 0 3 51 93 113 200 l114 194 -90 7 c-50 3 -132 6 -182 6
    l-92 0 -259 -437z"/>
        </g>
    </svg>
</DIV>

<DIV STYLE="text-align: center">
    <DIV STYLE="float:left">
        <IMG SRC="https://graph.facebook.com/{user.id}/picture?width=100&height=100">
    </DIV>
    <H1>
        <A HREF="/">taninim</A>
    </H1>
</DIV>

<DIV STYLE="clear:left">
    <HR>
</DIV>

<DIV STYLE="clear:left">
    <DIV STYLE="float: right">
        <A HREF="/unauth">Logg ut!</A>
    </DIV>
    <P>Hei, {user.name}! {length(media.albums)} plater, {length(media.tracks)} spor. Spilletid {media.prettyDuration}.
        Utgitt i perioden {media.startYear}-{media.endYear}.</P>

    <P>
        <SMALL>
            Tid til du kastes ut: {user.timeLeft} @ {user.timeRequested}.
            Data brukt: {user.mbStreamed}/{user.mbQuota}. Tilgangsniv&aring;: {user.prettyAccessLevel}
        </SMALL>
    </P>
</DIV>

{if(highlighted)}
<DIV STYLE="float: inside; text-align: center; padding:10px">
    {if(highlightedSelected)}Utvalgt spor:{else}Tilfeldig trall:{endif}
    <STRONG>{highlighted.name}</STRONG>
    <SMALL>
        <EM>{highlighted.prettyDuration}&nbsp;[{highlighted.prettyCompressedSize}]</EM>
        (Tilgjengelig i {highlightedRemaining})
    </SMALL>
</DIV>

<DIV STYLE="float:left; padding-right: 5px">
    <A HREF="{highlightedAlbum.context.discogPage}" TARGET="_blank">
        <IMG SRC="{highlightedAlbum.context.discogCover}" STYLE="width:50px ; height: 50px">
    </A>
</DIV>

<!--suppress HtmlUnknownAttribute -->
<AUDIO controls data-plyr-config='\{ "duration": {highlighted.seconds} }' id="player" preload="none"
       src="/audio/{highlighted.uuid}.m4a">
</AUDIO>

<DIV STYLE="float: inside; text-align: center; padding:10px">
    <SMALL>Fra <A HREF="/album?album={highlightedAlbum.uuid}"><EM>{highlighted.album}</EM></A>
        av
        <A HREF="/?{highlightedArtist.add}">{highlightedArtist.target.name}</A></SMALL>
</DIV>

{if(plyr)}
<SCRIPT src="/res/plyr.js"></SCRIPT>
<SCRIPT src="/res/plyr-setup.js"></SCRIPT>
{endif}
{endif}

<DIV STYLE="clear:both">
    {if(mediaCurations)}
    <DIV STYLE="float:left; padding-right: 10px">
        <H3>Kuratert</H3>
        <UL>
            {mediaCurations:{curation|
            <LI><A HREF="/?{curation.add}">{curation.target.name}</A></LI>
            }}
        </UL>
    </DIV>
    {endif}

    {if(mediaPlaylists)}
    <DIV STYLE="float:left">
        <H3>Spillelister</H3>
        <UL>
            {mediaPlaylists:{playlist|
            <LI><A HREF="/?{playlist.add}">{playlist.target.name}</A></LI>
            }}
        </UL>
    </DIV>
    {endif}

    {if(mediaSeries)}
    <DIV STYLE="float:right; padding-right: 10px">
        <H3>Serier</H3>
        <UL>
            {mediaSeries:{series|
            <LI><A HREF="/?{series.add}">{series.target.name}</A></LI>
            }}
        </UL>
    </DIV>
    {endif}
</DIV>

<DIV STYLE="clear:both">
    {if(artists)}
    {artists:{artist|
    [Artist&nbsp;<A HREF="/?{artist.focus}" TITLE="Vis bare denne"><STRONG>{
    artist.name
    }</STRONG></A>&nbsp;<A HREF="/?{artist.remove}" TITLE="Fjern denne">⏹</A>{
    if(user.streamingPlaylists)
    }<A HREF="/playlist/artist/{artist.target.uuid}" TITLE="Spilleliste for {artist.target.name}">⏬</A>{
    endif}]
    }}
    {endif}

    {if(series)}
    {series:{serie|
    [Serie&nbsp;<A HREF="/?{serie.focus}" TITLE="Vis bare denne"><STRONG>{
    serie.name
    }</STRONG></A>&nbsp;<A HREF="/?{serie.remove}" TITLE="Fjern denne">⏹</A>{
    if(user.streamingPlaylists)
    }<A HREF="/playlist/series/{serie.target.uuid}" TITLE="Spilleliste: {serie.target.name}">⏬</A>{
    endif}]
    }}
    {endif}

    {if(playlists)}
    {playlists:{playlist|
    [Spilleliste&nbsp;<A HREF="/?{playlist.focus}" TITLE="Vis bare denne"><STRONG>{
    playlist.name
    }</STRONG></A>&nbsp;<A HREF="/?{playlist.remove}" TITLE="Fjern denne">⏹</A>{
    if(user.streamingPlaylists)
    }<A HREF="/playlist/playlist/{playlist.target.uuid}" TITLE="Spilleliste {playlist.target.name}">⏬</A>{
    endif}]
    }}
    {endif}

    {if(curations)}
    {if(user.streamingCurated)}
    {curations:{curation|
    [Kuratert&nbsp<A HREF="/?{curation.focus}" TITLE="Vis bare denne"><STRONG>{
    curation.name
    }</STRONG></A>&nbsp;<A HREF="/?{curation.remove}" TITLE="Fjern denne">⏹</A>{
    if(user.streamingPlaylists)
    }<A HREF="/playlist/curation/{curation.target.uuid}" TITLE="Spilleliste {curation.target.name}">⏬</A>{
    endif}]
    }}
    {endif}
    {endif}

    <P>
    {if(unionLink)
    }Viser {if(union)
    }alle{
    else
    }tversnittet{
    endif
    }&nbsp;[<A HREF="/?{unionLink}">{
    if(union)
    }Vis tversnitt{
    else
    }Vis alle{
    endif
    }</A>]
    {endif}
</DIV>

<DIV STYLE="clear: both">
    <H3>
        Plater [{length(media.albums)}]
    </H3>

    <UL>
        {media.albumsByYear:{album|
        <LI>
            {if(user.streamingPlaylists)}
            <A HREF="/playlist/album/{album.uuid}" TITLE="Spilleliste: {album.name}">⏬</A>
            {endif}
            <A HREF="/album?album={album.uuid}">{album.artist.name}: <em>{album.name}</em></A>
            <sub><small>{album.context.year}</small></sub>
        </LI>
        }}
    </UL>
</DIV>

<HR>

{if(randomAlbums)}
<H3>Tilfeldige plater</H3>

<DIV STYLE="text-align: center">
    {randomAlbums:{album|
    <A HREF="/album?album={album.uuid}">
        <IMG ALT="{album.name}" SRC="{album.context.discogCover}" TITLE="{album.name}">
    </A>
    }}
</DIV>
{endif}

<DIV STYLE="clear: both">
    <P>
    <HR>
</DIV>

<DIV STYLE="clear: both">
    <H3>
        Artister [{length(media.allAlbumArtists)}]
    </H3>

    {albumArtists:{artist|
    [<A HREF="/?{artist.add}">{artist.target.name}</A>]
    }}
</DIV>

<DIV STYLE="clear: both">
    <P>
    <HR>
    <A HREF="res/privacy.txt">Privacy stuff</A> <A HREF="res/terms.txt">Terms of disservice</A>
</DIV>

</BODY>
</HTML>

