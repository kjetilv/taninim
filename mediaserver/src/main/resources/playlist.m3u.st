#EXTM3U
#PLAYLIST: {playlist.name}
{playlist.tracks:{track|

#EXTALB: {track.album}
#EXTART: {track.artist.name}
#EXTINF:{track.seconds};{track.prettyTrackNo} {track.name}
{protocol}://{host}/audio/{track.uuid}?streamlease={streamlease}
}}
