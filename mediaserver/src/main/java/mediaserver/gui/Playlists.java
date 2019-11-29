package mediaserver.gui;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import mediaserver.Media;
import mediaserver.files.Album;
import mediaserver.files.Artist;
import mediaserver.files.Playlist;
import mediaserver.files.Track;
import mediaserver.util.IO;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

public class Playlists extends Nettish {

    private static final String ALBUM = "/album/";

    private static final int ALBUM_PREAMBLE = ALBUM.length();

    private static final String ARTIST = "/artist/";

    private static final int ARTIST_PREAMBLE = ARTIST.length();

    private final Supplier<Media> media;

    private static final String AUDIO_MPEGURL = "audio/mpegurl";

    public Playlists(Supplier<Media> media) {

        super("/playlist");
        this.media = media;
    }

    @Override
    public HttpResponse handle(FullHttpRequest req, String path, ChannelHandlerContext ctx) {

        String resource = resource(path);
        if (resource.startsWith(ALBUM)) {
            return albumPlaylist(uuid(resource, ALBUM_PREAMBLE)).map(
                template ->
                    respond(ctx, response(req, null, AUDIO_MPEGURL, template.bytes(), null)))
                .orElseGet(() ->
                    respond(ctx, BAD_REQUEST));
        }
        if (resource.startsWith(ARTIST)) {
            return artistPlaylist(uuid(resource, ARTIST_PREAMBLE)).map(
                template ->
                    respond(ctx, response(req, null, AUDIO_MPEGURL, template.bytes(), null)))
                .orElseGet(() ->
                    respond(ctx, BAD_REQUEST));
        }
        return respond(ctx, BAD_REQUEST);
    }

    private UUID uuid(String path, int preamble) {

        return UUID.fromString(path.substring(preamble));
    }

    private Optional<Template> albumPlaylist(UUID albumUUID) {

        return media.get().getAlbum(albumUUID).map(this::playlist);
    }

    private Optional<Template> artistPlaylist(UUID artistUUID) {

        Media currentMedia = this.media.get();
        return currentMedia.getArtist(artistUUID).map(currentMedia::getTracksBy)
            .filter(tracks ->
                !tracks.isEmpty())
            .flatMap(tracks ->
                currentMedia.getArtist(artistUUID).map(a ->
                    this.playlist(a, tracks)));
    }

    private Template playlist(Album album) {

        return template("playlist.m3u")
            .add(QPar.PLAYLIST, new Playlist(album))
            .add(QPar.ARTIST, album.getArtist())
            .add(QPar.ALBUM, album);
    }

    private Template playlist(Artist artist, Collection<Track> tracks) {

        return template("playlist.m3u")
            .add(QPar.ARTIST, artist)
            .add(QPar.PLAYLIST, new Playlist(tracks));
    }
}
