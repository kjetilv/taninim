package mediaserver.gui;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import mediaserver.Media;
import mediaserver.files.Album;
import mediaserver.files.Artist;
import mediaserver.files.Playlist;
import mediaserver.files.Track;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

public class Playlists extends Nettish {

    public static final String LOCALHOST = "localhost";

    public static final String HTTPS = "https";

    private final Supplier<Media> media;

    private final Map<String, Function<UUID, Optional<Template>>> providers =
        Map.of(
            "/album/", this::albumPlaylist,
            "/artist/", this::artistPlaylist,
            "/category/", this::categoryPlaylist,
            "/series", this::seriesPlaylist);

    private static final String AUDIO_MPEGURL = "audio/mpegurl";

    public Playlists(Supplier<Media> media) {

        super("/playlist");
        this.media = media;
    }

    @Override
    public HttpResponse handle(FullHttpRequest req, String path, ChannelHandlerContext ctx) {

        String resource = resource(path);

        return templates(resource).findFirst()
            .map(template ->
                addProtocolAndHost(req, template))
            .map(template ->
                respond(ctx, response(req, null, AUDIO_MPEGURL, template.bytes(), null)))
            .orElseGet(() ->
                respond(ctx, BAD_REQUEST));
    }

    private Stream<Template> templates(String resource) {

        return providers.entrySet().stream()
            .filter(e ->
                resource.startsWith(e.getKey()))
            .map(e ->
                e.getValue().apply(uuid(resource, e.getKey().length())))
            .flatMap(Optional::stream);
    }

    private Template addProtocolAndHost(HttpRequest req, Template template) {

        return template
            .add(
                QPar.HOST,
                Optional.ofNullable(req.headers().getAsString(HttpHeaderNames.HOST)).orElse(LOCALHOST))
            .add(
                QPar.PROTOCOL,
                Optional.ofNullable(req.uri()).map(URI::create).map(URI::getScheme).orElse(HTTPS));
    }

    private UUID uuid(String path, int preamble) {

        return UUID.fromString(path.substring(preamble));
    }

    private Optional<Template> albumPlaylist(UUID albumUUID) {

        Media med = this.media.get();
        return med.getAlbum(albumUUID).map(this::playlist);
    }

    private Optional<Template> categoryPlaylist(UUID categoryUUID) {

        Media media = this.media.get();
        return media.getCategoryPath(categoryUUID)
            .map(media::subLibrary)
            .map(subMedia ->
                subMedia.getAlbums(true))
            .map(this::playlist);
    }

    private Optional<Template> seriesPlaylist(UUID seriesUUID) {

        Media media = this.media.get();
        return media.getSeries(seriesUUID)
            .map(media::subLibrary)
            .map(subMedia ->
                subMedia.getAlbums(true))
            .map(this::playlist);
    }

    private Optional<Template> artistPlaylist(UUID artistUUID) {

        Media media = this.media.get();
        return media.getArtist(artistUUID)
            .map(media::getTracksFeaturing)
            .filter(tracks -> !tracks.isEmpty())
            .flatMap(tracks ->
                media.getArtist(artistUUID).map(artist ->
                    this.playlist(artist, tracks)));
    }

    private Template playlist(Album album) {

        return template("res/playlist.m3u")
            .add(QPar.PLAYLIST, new Playlist(album));
    }

    private Template playlist(Collection<Album> albums) {

        return template("res/playlist.m3u")
            .add(QPar.PLAYLIST, new Playlist(
                albums.size() + " albums",
                albums.stream().map(Album::getTracks).flatMap(Collection::stream).collect(Collectors.toList())));
    }

    private Template playlist(Artist artist, Collection<Track> tracks) {

        return template("res/playlist.m3u")
            .add(QPar.PLAYLIST, new Playlist(artist.getName(), tracks));
    }
}
