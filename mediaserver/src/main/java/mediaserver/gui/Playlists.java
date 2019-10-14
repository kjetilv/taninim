package mediaserver.gui;

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

    public Playlists(IO io, Media media) {
        super(io, "/playlist");
        this.media = media;
    }

    @Override
    public HttpResponse handle(HttpRequest req, String path, ChannelHandlerContext ctx) {
        String resource = resource(path);
        if (resource.startsWith(ALBUM)) {
            return albumPlaylist(uuid(resource, ALBUM_PREAMBLE)).map(
                template ->
                    respond(ctx, path, response(req, AUDIO_MPEGURL, template.bytes())))
                .orElseGet(() ->
                    respond(ctx, path, BAD_REQUEST));
        }
        if (resource.startsWith(ARTIST)) {
            return artistPlaylist(uuid(resource, ARTIST_PREAMBLE)).map(
                template ->
                    respond(ctx, path, response(req, AUDIO_MPEGURL, template.bytes())))
                .orElseGet(() ->
                    respond(ctx, path, BAD_REQUEST));
        }
        return respond(ctx, path, BAD_REQUEST);
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
            .add(QPar.PLAYLIST, new Playlist(album))
            .add(QPar.ARTIST, album.getArtist())
            .add(QPar.ALBUM, album)
            .add(QPar.HOST, resolve(""));
    }

    private Template playlist(Artist artist, Collection<Track> tracks) {
        return template("playlist.m3u")
            .add(QPar.ARTIST, artist)
            .add(QPar.HOST, resolve(""))
            .add(QPar.PLAYLIST, new Playlist(tracks));
    }
}
