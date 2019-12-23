package mediaserver.gui;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import mediaserver.http.Handling;
import mediaserver.http.Prefix;
import mediaserver.http.QPar;
import mediaserver.http.WebPath;
import mediaserver.media.*;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

public final class PlaylistsM3U extends TemplateEnabled {

    public static final String LOCALHOST = "localhost";

    public static final String HTTPS = "https";

    private final Supplier<Media> media;

    private final Map<String, Function<UUID, Optional<Template>>> providers =
        Map.of(
            "/album/", this::albumSublibrary,
            "/playlist/", this::playlistSublibrary,
            "/artist/", this::artistPlaylist,
            "/series", this::seriesSublibrary);

    private static final String AUDIO_MPEGURL = "audio/mpegurl";

    public PlaylistsM3U(Supplier<Media> media, Templater templater) {

        super(templater, Prefix.PLAYLIST);
        this.media = media;
    }

    @Override
    public Handling handleRequest(FullHttpRequest req, WebPath webPath, ChannelHandlerContext ctx) {

        String resource = resource(webPath);

        return templates(resource).findFirst()
            .map(template ->
                addProtocolAndHost(req, template))
            .map(template ->
                respond(ctx, response(req, null, AUDIO_MPEGURL, template.bytes(), null)))
            .orElseGet(() ->
                respond(ctx, NOT_FOUND));
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

    private Optional<Template> albumSublibrary(UUID albumUUID) {

        Media med = this.media.get();
        return med.getAlbum(albumUUID).map(this::playlist);
    }

    private Optional<Template> seriesSublibrary(UUID seriesUUID) {

        Media media = this.media.get();
        return media.getSeries(seriesUUID)
            .map(media::subLibrary)
            .map(subMedia ->
                subMedia.getAlbums(true))
            .map(this::playlist);
    }

    private Optional<Template> playlistSublibrary(UUID playlistUUID) {

        Media media = this.media.get();
        return media.getPlaylist(playlistUUID)
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
            .add(QPar.PLAYLIST, new PlaylistM3U(album));
    }

    private Template playlist(Collection<Album> albums) {

        return template("res/playlist.m3u")
            .add(QPar.PLAYLIST, new PlaylistM3U(
                albums.size() + " albums",
                albums.stream().map(Album::getTracks).flatMap(Collection::stream).collect(Collectors.toList())));
    }

    private Template playlist(Artist artist, Collection<Track> tracks) {

        return template("res/playlist.m3u")
            .add(QPar.PLAYLIST, new PlaylistM3U(artist.getName(), tracks));
    }
}
