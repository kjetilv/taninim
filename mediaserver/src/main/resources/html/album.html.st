<!doctype html>
<!--suppress HtmlUnknownTarget -->
<HTML lang="en">
<HEAD>
    <LINK HREF="/res/taninim.css" REL="stylesheet">
    {if(plyr)}
    <LINK HREF="/res/plyr.css" REL="stylesheet">
    {endif}
    <META charset="utf-8">
    <TITLE>taninim[{album.name}]</TITLE>
</HEAD>
<BODY>

<DIV STYLE="float: right">
    <svg height="130pt" version="1.0"
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
    <P>Hei, {user.name}! </P>
    <P>
        <SMALL>
            Tid til du kastes ut: {user.timeLeft} @ {user.timeRequested}.
            Data brukt: {user.mbStreamed}/{user.mbQuota}. Tilgangsniv&aring;: {user.prettyAccessLevel}
        </SMALL>
    </P>
</DIV>

<DIV STYLE="float:left; width: 100%">
    <DIV STYLE="float: right; padding: 1% 1% 1% 3%;">
        {if(album.series)}
        <H4>I serie</H4>
        <UL>
            {album.series:{series|
            <LI><A HREF="/?series={series.uuid}">{series.name}</A></LI>
            }}
        </UL>
        {endif}
        {if(playlists)}
        <H4>I spilleliste</H4>
        <UL>
            {playlists:{playlist|
            <LI><A HREF="/?playlist={playlist.uuid}">{playlist.name}</A></LI>
            }}
        </UL>
        {endif}
        {if(curations)}
        <H4>I kuratert spilleliste</H4>
        <UL>
            {curations:{curation|
            <LI><A HREF="/?curation={curation.uuid}">{curation.name}</A></LI>
            }}
        </UL>
        {endif}
    </DIV>
    <DIV>
        {if(album.context.discogCover)}
        <DIV STYLE="float: left; padding-right: 20px">
            <A HREF="{album.context.discogPage}" TARGET="_blank"><IMG SRC="{album.context.discogCover}"></A>
        </DIV>
        {endif}

        <H3><A HREF="?artist={album.artist.uuid}">{album.artist.name}</A>: <EM>{album.name}</EM>
            {if(albumPlayable)}
            <A href="?artist={album.artist.uuid}&album={album.uuid}&track={albumPlayable.track.uuid}&autoplay={albumPlayable.track.uuid}">
                ▶️
            </A>
            {endif}

            {if(user.streamingPlaylists)}
            <A HREF="/playlist/album/{album.uuid}">⏬</A>
            {endif}
            <SUB>({album.context.year})</SUB>
        </H3>

        <DIV>
            <DIV>
                <P>
                    Spilletid {album.prettyDuration}{if(album.context.year)}.{endif}
                </P>
            </DIV>
            {if(selected)}
            {endif}
        </DIV>

        <DIV STYLE="clear: both">
            {if(selected)}
            <DIV STYLE="float: inside; text-align: center; padding:10px">
                {selected.track.prettyTrackNo}:&nbsp;<STRONG>{selected.track.name}</STRONG>
                <SMALL>
                    <EM>{selected.track.prettyDuration}&nbsp;[{
                        if(compressed)}{selected.track.prettyCompressedSize}
                        {else}{selected.track.prettyFileSize}
                        {endif} {format}]</EM>
                </SMALL>
            </DIV>

            <!--suppress HtmlUnknownAttribute -->
            <AUDIO controls
                   data-plyr-config='\{ "duration": {selected.track.seconds}, "autoplay": {selected.autoplay} }'
                   id="player" {if(selected.autoplay)}preload="auto"{else}preload="none"{endif}
                   src="/audio/{selected.track.uuid}.{format}">
            </AUDIO>

            <DIV STYLE="float: right; text-align: end; width: 30%">
                {if(selected.next)}
                <A HREF="?artist={album.artist.uuid}&album={album.uuid}&track={selected.next.uuid}&autoplay={selected.next.uuid}" ID='play-next'></A>
                <SMALL>
                    <A HREF="?artist={album.artist.uuid}&album={album.uuid}&track={selected.next.uuid}&autoplay={selected.next.uuid}">
                        {selected.next.prettyTrackNo}:&nbsp;{selected.next.name}&nbsp;⏩&nbsp;</A>
                </SMALL>
                {endif}
            </DIV>

            {if(selected.previous)}
            <DIV STYLE="float:left; text-align: start; width: 30%">
                <SMALL>
                    <A HREF="?artist={album.artist.uuid}&album={album.uuid}&track={selected.previous.uuid}&autoplay={selected.previous.uuid}">
                        &nbsp;⏪&nbsp;{selected.previous.prettyTrackNo}:&nbsp;{selected.previous.name}</A>
                </SMALL>
            </DIV>
            {endif}

            {if(admin)}
            <FORM ACTION="/admin/{if(trackHighlighted)}n{else}j{endif}uke" METHOD="POST">
                <DIV STYLE="clear:both; float: right">
                    <INPUT NAME="jukeboxTrack" TYPE="hidden" value="{selected.track.uuid}">
                    <INPUT NAME="jukeboxAlbum" TYPE="hidden" value="{selected.album.uuid}">
                    <INPUT NAME="jukeboxClear" TYPE="hidden" value="{trackHighlighted}">
                    <BUTTON TYPE="SUBMIT" VALUE="jukebox">
                        {if(trackHighlighted)}N{else}J{endif}uke!
                    </BUTTON>
                </DIV>
            </FORM>
            {endif}

            {if(plyr)}
            <script src="/res/plyr.js"></script>
            <SCRIPT src="/res/plyr-setup.js"></SCRIPT>
            {endif}
            {endif}
        </DIV>

        <DIV STYLE="padding-top: 1%; padding-left: 1%; clear: left">
            {playableGroups:{playableGroup|
            <DIV STYLE="padding-top: 2%">
                <H4>{if(playableGroup.name)}Plate {playableGroup.name}:{endif}</H4>
            </DIV>
            {playableGroup.playables:{playable |
            <!--suppress HtmlUnknownAttribute -->
            <DIV STYLE="padding: 0.2%; clear: both">
                {if(playable.playable)}
                <A href="?artist={album.artist.uuid}&album={album.uuid}&track={playable.track.uuid}&autoplay={playable.track.uuid}">
                    <DIV STYLE="float: left; width:8%; text-align: end; padding-right: 1%">
                        <EM><SMALL>{playable.track.prettyTrackNo}.</SMALL></EM>
                    </DIV>
                    <DIV STYLE="width:8%; float: left; text-align: center" class="playable-button">
                        ▶️
                    </DIV>
                    <DIV STYLE="width: fit-content; float: left" class="playable-track">
                        {playable.track.name}
                    </DIV>
                    <DIV STYLE="width:15%; float: right">
                        <EM>
                            <SMALL>{playable.track.prettyDuration}/{
                                if(compressed)}{playable.track.prettyCompressedSize}
                                {else}{playable.track.prettyFileSize}{endif
                                } {format}</SMALL>
                        </EM>
                    </DIV>
                </A>
                {else}
                <DIV STYLE="float: left; width:9%; text-align: end; padding-right: 1%">
                    <EM>{playable.track.prettyTrackNo}.</EM>
                </DIV>
                <DIV STYLE="width: fit-content; float: left">
                    {playable.track.name}
                </DIV>
                <DIV STYLE="width:5%; float: right">
                    <EM>
                        <SMALL>
                            {playable.track.prettyCompressedSize}
                        </SMALL>
                    </EM>
                </DIV>
                <DIV STYLE="width:5%; float: right">
                    <EM>
                        <SMALL>
                            {playable.track.prettyDuration}
                        </SMALL>
                    </EM>
                </DIV>
                {endif}
            </DIV>
            {if(playable.track.otherArtists)}
            <DIV STYLE="clear: both;padding-left: 10%">
                <UL>
                    {playable.track.otherArtists:{otherArtist|
                    <LI>
                        [&nbsp;<A HREF="/?artist={otherArtist.uuid}">{otherArtist.name}</A>{
                        if(user.streamingPlaylists)
                        }&nbsp;<A HREF="/playlist/artist/{otherArtist.uuid}">⏬</A>{endif}&nbsp;]
                    </LI>
                    }}
                </UL>
            </DIV>
            {endif}
            }}
            }}
            <P>
        </DIV>
    </DIV>
    <DIV STYLE="clear: both; padding-top: 2%">
        <HR>
    </DIV>
    <DIV STYLE="">
        {if(album.context.credits.credits)}
        <DIV STYLE="float: left">
            <H3>Utøvere</H3>
            <UL>
                {album.context.credits.artistCredits:{credit |
                <LI>{if(credit.sourceType)}{credit.sourceType}: {endif}
                    [&nbsp;<A HREF="/?artist={credit.artist.uuid}">{credit.name}</A>
                    {if(user.streamingPlaylists)}
                    &nbsp;<A HREF="/playlist/artist/{credit.artist.uuid}">⏬</A>{endif}&nbsp;]
                </LI>
                }}
            </UL>
        </DIV>
        <DIV STYLE="float: right">
            <H3>Andre involverte</H3>

            <UL>
                {album.context.credits.otherCredits:{credit|
                <LI>{if(credit.sourceType)}{credit.sourceType}: {endif}{credit.name}</LI>
                }}
            </UL>
        </DIV>

        {endif}
    </DIV>

    <DIV STYLE="clear: both">
        {if(album.context.additionalTrackContext)}

        <H3>Spormengder</H3>

        {album.context.trackGroups:{trackGroup |
        <H4>{trackGroup.name}</H4>
        <UL>
            {trackGroup.trackContexts:{trackContext |
            <LI>{trackContext.prettyTrackNo} {trackContext.title}
                {if(trackContext.credits.artistCredits)}
                <UL>
                    {trackContext.credits.artistCredits:{credit |
                    <LI>{if(credit.sourceType)}{credit.sourceType}: {endif}
                        [<A HREF="/?artist={credit.artist.uuid}">{credit.name}</A>
                        {if(user.streamingPlaylists)}
                        <A HREF="/playlist/artist/{credit.artist.uuid}"><SMALL>⏬</SMALL></A>
                        {endif}
                        ]
                    </LI>
                    }}
                </UL>
                {endif}
                {if(trackContext.credits.otherCredits)}
                <UL>
                    {trackContext.credits.otherCredits:{credit |
                    <LI>
                        {if(credit.sourceType)}{credit.sourceType}: {endif}{credit.name}
                    </LI>
                    }}
                </UL>
                {endif}
            </LI>
            }}
        </UL>
        }}
        {endif}
    </DIV>

    <DIV STYLE="clear: both">
        {if(album.additionalArtists)}
        <H3>Medvirkende</H3>

        {album.artists:{artist|[
        <A HREF="/?artist={artist.uuid}">{artist.name}</A>&nbsp;
        {if(user.streamingPlaylists)}
        <A HREF="/playlist/artist/{artist.uuid}"><SMALL>⏬</SMALL></A>
        {endif}
        ]
        }}
        {endif}
    </DIV>

    <DIV STYLE="clear: both">
        <H2>Info</H2>

        {if(album.context.notes)}
        <P>Notert:</P>
        <blockquote>{album.context.notes}</blockquote>
        {endif}

        {if(album.context.discogPage)}
        <P>@ <A HREF="{album.context.discogPage}" TARGET="_blank">Discogs</A></P>
        {endif}
    </DIV>

</DIV>
<DIV STYLE="clear: both">
    <HR>
    <A HREF="res/privacy.txt">Privacy stuff</A> <A HREF="res/terms.txt">Terms of disservice</A>
</DIV>

{if(selected)}
{if(selected.next)}
<SCRIPT src="/res/autonext.js"></SCRIPT>
{endif}
{endif}

</BODY>
</HTML>
