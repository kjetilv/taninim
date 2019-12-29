package mediaserver.gui;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponse;
import mediaserver.http.*;
import mediaserver.media.*;
import mediaserver.sessions.Session;
import mediaserver.sessions.Sessions;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

public final class M3UPlaylists extends TemplateEnabled {

    private final Supplier<Media> media;

    private final Sessions sessions;

    private final boolean https;

    private final Map<String, Function<UUID, Optional<Template>>> providers =
        Map.of(
            "/artist/", this::artistPlaylist,
            "/album/", this::albumSublibrary,
            "/playlist/", this::playlistSublibrary,
            "/series/", this::seriesSublibrary);

    private static final String CONTENT_TYPE = "audio/x-mpegurl";

    public M3UPlaylists(
        Supplier<Media> media,
        Sessions sessions,
        Templater templater,
        boolean https
    ) {

        super(templater, Prefix.PLAYLIST);
        this.media = media;
        this.sessions = sessions;
        this.https = https;
    }

    @Override
    public Handling handleRequest(WebPath webPath, ChannelHandlerContext ctx) {

        return sessions.activeSession(webPath)
            .map(session ->
                template(webPath.getUri())
                    .map(template ->
                        instrumented(template, session, webPath))
                    .map(template ->
                        sendResponse(ctx, response(webPath, template)))
                    .orElseGet(() ->
                        sendResponse(ctx, NOT_FOUND)))
            .orElseGet(() ->
                sendResponse(ctx, UNAUTHORIZED));
    }

    private HttpResponse response(WebPath webPath, Template template) {

        return Netty.response(webPath, CONTENT_TYPE, OK, template.bytes());
    }

    private Optional<Template> template(String resource) {

        return providers.entrySet().stream()
            .filter(e ->
                resource.startsWith(e.getKey()))
            .map(e ->
                provide(e.getKey(), e.getValue(), resource))
            .flatMap(Optional::stream)
            .findFirst();
    }

    private Optional<Template> provide(String heading, Function<UUID, Optional<Template>> provider, String resource) {

        return provider.apply(uuid(resource, heading.length()));
    }

    private Template instrumented(Template template, Session session, WebPath webPath) {

        return template
            .add(QPar.STREAMLEASE, session.getCookie())
            .add(QPar.HOST, webPath.getHost())
            .add(QPar.PROTOCOL, https ? "https" : "http");
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

        return readTemplate("res/playlist.m3u")
            .add(QPar.PLAYLIST, new PlaylistM3U(album));
    }

    private Template playlist(Collection<Album> albums) {

        return readTemplate("res/playlist.m3u")
            .add(QPar.PLAYLIST, new PlaylistM3U(
                albums.size() + " albums",
                albums.stream().map(Album::getTracks).flatMap(Collection::stream).collect(Collectors.toList())));
    }

    private Template playlist(Artist artist, Collection<Track> tracks) {

        return readTemplate("res/playlist.m3u")
            .add(QPar.PLAYLIST, new PlaylistM3U(artist.getName(), tracks));
    }
}
