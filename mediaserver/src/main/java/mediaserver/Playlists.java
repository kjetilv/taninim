package mediaserver;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import mediaserver.files.Album;
import mediaserver.files.Media;
import mediaserver.files.Playlist;
import mediaserver.files.Track;
import mediaserver.util.IO;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

public class Playlists extends Nettish {

    private static final String ALBUM = "/album/";

    private static final int ALBUM_PREAMBLE = ALBUM.length();

    private static final String ARTIST = "/artist/";

    private static final int ARTIST_PREAMBLE = ARTIST.length();

    private final Media media;

    private static final String AUDIO_MPEGURL = "audio/mpegurl";


    Playlists(IO io, Media media) {
        super(io, "/playlist");
        this.media = media;
    }

    @Override
    void handle(HttpRequest req, String path, ChannelHandlerContext ctx) {
        if (path.startsWith(ALBUM)) {
            albumPlaylist(albumUUID(path)).ifPresentOrElse(
                template ->
                    respond(ctx, response(req, AUDIO_MPEGURL, template.bytes())),
                () ->
                    respond(ctx, BAD_REQUEST));
        } else if (path.startsWith(ARTIST)) {
            artistPlaylist(artistName(path)).ifPresentOrElse(
                template ->
                    respond(ctx, response(req, AUDIO_MPEGURL, template.bytes())),
                () ->
                    respond(ctx, BAD_REQUEST));
        } else {
            respond(ctx, BAD_REQUEST);
        }
    }

    private UUID albumUUID(String path) {
        return UUID.fromString(path.substring(ALBUM_PREAMBLE));
    }

    private String artistName(String path) {
        return path.substring(ARTIST_PREAMBLE);
    }

    private Optional<Template> albumPlaylist(UUID albumUUID) {
        return media.getAlbum(albumUUID).map(this::playlist);
    }

    private Optional<Template> artistPlaylist(String artist) {
        return Optional.ofNullable(media.getTracksBy(artist))
            .filter(tracks ->
                !tracks.isEmpty())
            .map(tracks ->
                this.playlist(artist, tracks));
    }

    private Template playlist(Album album) {
        return template("playlist.m3u")
            .add("playlist", new Playlist(album))
            .add("artist", album.getArtist())
            .add("album", album)
            .add("host", resolve(""));
    }

    private Template playlist(String artist, Collection<Track> tracks) {
        return template("playlist.m3u")
            .add("artist", artist)
            .add("host", resolve(""))
            .add("playlist", new Playlist(tracks));
    }
}
