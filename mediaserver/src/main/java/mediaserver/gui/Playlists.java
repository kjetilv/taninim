package mediaserver.gui;

import io.netty.handler.codec.http.HttpResponse;
import mediaserver.http.*;
import mediaserver.media.*;
import mediaserver.sessions.AccessLevel;
import mediaserver.templates.TPar;
import mediaserver.templates.Template;
import mediaserver.toolkit.Templater;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public final class Playlists extends TemplateEnabled {

    private final Supplier<Media> media;

    private final boolean https;

    private final Map<String, Function<UUID, Optional<Template>>> providers = Map.of(
        "/artist/", this::artistPlaylist,
        "/album/", this::albumSublibrary,
        "/playlist/", this::playlistSublibrary,
        "/series/", this::seriesSublibrary);

    private static final String CONTENT_TYPE = "audio/x-mpegurl";

    public Playlists(Supplier<Media> media, Templater templater, boolean https) {

        super(templater, Page.PLAYLIST);
        this.media = media;
        this.https = https;
    }

    @Override
    protected Handling handleRequest(Req req) {

        if (req.getSession().hasLevel(AccessLevel.STREAM)) {
            template(req.getUri())
                .map(template ->
                    instrumented(template, req))
                .map(template ->
                    respond(req, response(req, template)))
                .orElseGet(() ->
                    handleNotFound(req));
        }
        return handleBadRequest(req);
    }

    private HttpResponse response(Req req, Template template) {

        return Netty.response(req, CONTENT_TYPE, OK, template.bytes());
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

    private Template instrumented(Template template, Req req) {

        return template
            .add(TPar.STREAMLEASE, req.getSession().getCookie())
            .add(TPar.HOST, req.getHost())
            .add(TPar.PROTOCOL, https ? "https" : "http");
    }

    private UUID uuid(String path, int preamble) {

        return UUID.fromString(path.substring(preamble));
    }

    private Optional<Template> albumSublibrary(UUID albumUUID) {

        return this.media.get().getAlbum(albumUUID).map(this::playlist);
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

        return getTemplate(PLAYLIST_M3U).add(TPar.PLAYLIST, new PlaylistM3U(album));
    }

    private Template playlist(Artist artist, Collection<Track> tracks) {

        return getTemplate(PLAYLIST_M3U).add(TPar.PLAYLIST, new PlaylistM3U(artist.getName(), tracks));
    }

    private Template playlist(Collection<Album> albums) {

        return getTemplate(PLAYLIST_M3U).add(
            TPar.PLAYLIST,
            new PlaylistM3U(
                albums.size() + " albums",
                albums.stream().map(Album::getTracks).flatMap(Collection::stream).collect(Collectors.toList())));
    }
}
