package mediaserver;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import mediaserver.files.*;
import mediaserver.util.IO;

import java.util.Collection;
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
    HttpResponse handle(HttpRequest req, String path, ChannelHandlerContext ctx) {
        if (path.startsWith(ALBUM)) {
            return albumPlaylist(uuid(path, ALBUM_PREAMBLE)).map(
                template ->
                    respond(ctx, response(req, AUDIO_MPEGURL, template.bytes())))
                .orElseGet(() ->
                    respond(ctx, BAD_REQUEST));
        }
        if (path.startsWith(ARTIST)) {
            return artistPlaylist(uuid(path, ARTIST_PREAMBLE)).map(
                template ->
                    respond(ctx, response(req, AUDIO_MPEGURL, template.bytes())))
                .orElseGet(() ->
                    respond(ctx, BAD_REQUEST));
        }
        return respond(ctx, BAD_REQUEST);
    }

    private UUID uuid(String path, int preamble) {
        return UUID.fromString(path.substring(preamble));
    }

    private Optional<Template> albumPlaylist(UUID albumUUID) {
        return media.getAlbum(albumUUID).map(this::playlist);
    }

    private Optional<Template> artistPlaylist(UUID artistUUID) {
        return media.getArtist(artistUUID).map(media::getTracksBy)
            .filter(tracks ->
                !tracks.isEmpty())
            .flatMap(tracks ->
                media.getArtist(artistUUID).map(a ->
                    this.playlist(a, tracks)));
    }

    private Template playlist(Album album) {
        return template("playlist.m3u")
            .add("playlist", new Playlist(album))
            .add("artist", album.getArtist())
            .add("album", album)
            .add("host", resolve(""));
    }

    private Template playlist(Artist artist, Collection<Track> tracks) {
        return template("playlist.m3u")
            .add("artist", artist)
            .add("host", resolve(""))
            .add("playlist", new Playlist(tracks));
    }
}
