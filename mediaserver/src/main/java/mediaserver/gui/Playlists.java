package mediaserver.gui;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import mediaserver.http.*;
import mediaserver.media.*;
import mediaserver.sessions.AccessLevel;
import mediaserver.templates.TPar;
import mediaserver.templates.Template;
import mediaserver.toolkit.Templater;
import mediaserver.util.Pair;
import mediaserver.util.Print;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static mediaserver.gui.Playlists.PlaylistProvider.*;

public final class Playlists extends TemplateEnabled {

    private final Supplier<Media> media;

    private final boolean https;

    private final Map<PlaylistProvider, Function<UUID, Optional<Pair<String, Template>>>> providers = Map.of(
        artist, this::artistPlaylist,
        album, this::albumSublibrary,
        playlist, this::playlistSublibrary,
        curation, this::curationSublibrary,
        series, this::seriesSublibrary
    );

    private static final String AUDIO_X_MPEGURL = "audio/x-mpegurl";

    public Playlists(Supplier<Media> media, Templater templater, boolean https) {

        super(templater, Route.PLAYLIST);
        this.media = media;
        this.https = https;
    }

    @Override
    protected Handling handle(Req req) {

        if (req.getSession().hasLevel(AccessLevel.STREAM)) {
            String resource = req.getUri();
            providers.entrySet().stream()
                .filter(e ->
                    e.getKey().test(resource))
                .map(e ->
                    e.getValue().apply(UUID.fromString(e.getKey().suffix(resource)))
                        .map(nameAndTemplate ->
                            playlistResponse(req, nameAndTemplate.getT1(), nameAndTemplate.getT2())))
                .flatMap(Optional::stream)
                .findFirst()
                .map(httpResponse ->
                    handle(req, httpResponse))
                .orElseGet(() ->
                    handleNotFound(req));
        }
        return handleBadRequest(req);
    }

    private HttpResponse playlistResponse(Req req, String t1, Template t2) {

        return Netty.response(
            req,
            AUDIO_X_MPEGURL,
            OK,
            t2
                .add(TPar.streamlease, req.getSession().getCookie())
                .add(TPar.host, req.getHost())
                .add(TPar.protocol, https ? "https" : "http")
                .bytes(),
            setFilename(t1, req.getSession().getCookie()));
    }

    private static Headers setFilename(String template, UUID cookie) {

        return headers ->
            headers.accept(
                HttpHeaderNames.CONTENT_DISPOSITION,
                "inline; filename=\"" + template + '-' + Print.uuid(cookie) + "-playlist.m3u\"");
    }

    private Optional<Pair<String, Template>> albumSublibrary(UUID albumUUID) {

        return this.media.get().getAlbum(albumUUID)
            .map(album ->
                Pair.of(album.getName(), playlist(album)));
    }

    private Optional<Pair<String, Template>> seriesSublibrary(UUID seriesUUID) {

        Media media = this.media.get();
        return media.getSeries(seriesUUID)
            .flatMap(series ->
                Optional.of(media.subLibrary(series))
                    .map(submedia ->
                        Pair.of(
                            series.getName(),
                            playlist(
                                PlaylistProvider.series,
                                series.getName(), submedia.getAlbums(true)))));
    }

    private Optional<Pair<String, Template>> playlistSublibrary(UUID playlistUUID) {

        Media media = this.media.get();
        return media.getPlaylist(playlistUUID)
            .flatMap(playlist ->
                Optional.ofNullable(media.subLibrary(playlist))
                    .map(submedia ->
                        Pair.of(
                            playlist.getName(),
                            playlist(
                                PlaylistProvider.playlist,
                                playlist.getName(),
                                submedia.getAlbums(true)))));
    }

    private Optional<Pair<String, Template>> curationSublibrary(UUID curationUUID) {

        Media media = this.media.get();
        return media.getCuration(curationUUID)
            .flatMap(curation ->
                Optional.ofNullable(media.subLibrary(curation))
                    .map(submedia ->
                        Pair.of(
                            curation.getName(),
                            playlist(
                                PlaylistProvider.curation,
                                curation.getName(),
                                submedia.getAlbums(true)))));
    }

    private Optional<Pair<String, Template>> artistPlaylist(UUID artistUUID) {

        Media media = this.media.get();
        return media.getArtist(artistUUID)
            .flatMap(artist ->
                Optional.ofNullable(media.getTracksFeaturing(artist))
                    .filter(tracks ->
                        !tracks.isEmpty())
                    .map(tracks ->
                        Pair.of(
                            artist.getName(),
                            playlist(
                                artist,
                                tracks))));
    }

    private Template playlist(Album album) {

        return baseTemplate(
            new PlaylistM3U(album.getArtist().getName() + ": " + album.getName(), album.getTracks()));
    }

    private Template playlist(Artist artist, Collection<Track> tracks) {

        return baseTemplate(
            new PlaylistM3U(
                PlaylistProvider.artist + ": " + artist.getName() + " (" + tracks.size() + " tracks)",
                tracks));
    }

    private Template playlist(PlaylistProvider type, String name, Collection<Album> albums) {

        List<Track> tracks = albums.stream().map(Album::getTracks).flatMap(Collection::stream).collect(
            Collectors.toList());
        return baseTemplate(
            new PlaylistM3U(
                type + ": " + name + " (" + albums.size() + " albums, " + tracks.size() + " tracks)",
                tracks));
    }

    private Template baseTemplate(PlaylistM3U value) {

        return getTemplate(PLAYLIST_M3U).add(
            TPar.playlist,
            value);
    }

    enum PlaylistProvider implements Predicate<String> {

        artist,

        album,

        playlist,

        curation,

        series;

        private final String path;

        PlaylistProvider() {

            path = '/' + name() + '/';
        }

        @Override
        public boolean test(String resource) {

            return resource.startsWith(path);
        }

        public String suffix(String resource) {

            return resource.substring(path.length());
        }

        @Override
        public String toString() {

            return name().substring(0, 1).toUpperCase() + name().substring(1);
        }
    }
}
