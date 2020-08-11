package mediaserver.gui;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import mediaserver.http.Handling;
import mediaserver.http.Headers;
import mediaserver.http.Netty;
import mediaserver.http.Req;
import mediaserver.http.Route;
import mediaserver.media.Album;
import mediaserver.media.Artist;
import mediaserver.media.Media;
import mediaserver.media.PlaylistM3U;
import mediaserver.media.Track;
import mediaserver.sessions.AccessLevel;
import mediaserver.templates.TPar;
import mediaserver.templates.Template;
import mediaserver.toolkit.Templater;
import mediaserver.util.Pair;
import mediaserver.util.Print;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static mediaserver.gui.Playlists.PlaylistProvider.album;
import static mediaserver.gui.Playlists.PlaylistProvider.artist;
import static mediaserver.gui.Playlists.PlaylistProvider.curation;
import static mediaserver.gui.Playlists.PlaylistProvider.playlist;
import static mediaserver.gui.Playlists.PlaylistProvider.series;

public final class Playlists extends TemplateEnabled {
    
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
        
        private String suffix(String resource) {
            return resource.substring(path.length());
        }
        
        @Override
        public String toString() {
            return name().substring(0, 1).toUpperCase() + name().substring(1);
        }
    }
    
    private final Supplier<Media> media;
    
    private final boolean https;
    
    private final Map<PlaylistProvider, Function<UUID, Stream<Pair<String, Template>>>> providers = Map.of(
        artist, this::artistPlaylist,
        album, this::albumSublibrary,
        playlist, this::playlistSublibrary,
        curation, this::curationSublibrary,
        series, this::seriesSublibrary);
    
    public Playlists(
        Route route,
        Supplier<Media> media,
        Templater templater,
        boolean https
    ) {
        super(route, templater);
        this.media = Objects.requireNonNull(media, "media");
        this.https = https;
    }
    
    protected @Override
    Handling handle(Req req) {
        if (req.getSession().hasLevel(AccessLevel.STREAM)) {
            String resource = req.getUri();
            providers.entrySet()
                .stream()
                .filter(e -> e.getKey().test(resource))
                .flatMap(e -> e.getValue()
                    .apply(UUID.fromString(e.getKey().suffix(resource)))
                    .map(nameAndTemplate -> playlistResponse(req, nameAndTemplate.getT1(), nameAndTemplate.getT2())))
                .map(httpResponse -> handle(req, httpResponse))
                .findFirst()
                .orElseGet(() -> handleNotFound(req));
        }
        return handleBadRequest(req);
    }
    
    private HttpResponse playlistResponse(Req req, String t1, Template t2) {
        return Netty.response(
            req,
            AUDIO_X_MPEGURL,
            OK,
            t2.add(TPar.streamlease, req.getSession().getCookie())
                .add(TPar.host, req.getHost())
                .add(TPar.protocol, https ? "https" : "http")
                .bytes(),
            setFilename(t1, req.getSession().getCookie()));
    }
    
    private Stream<Pair<String, Template>> albumSublibrary(UUID albumUUID) {
        return this.media.get().getAlbum(albumUUID).map(album -> Pair.of(album.getName(), playlist(album)));
    }
    
    private Stream<Pair<String, Template>> seriesSublibrary(UUID seriesUUID) {
        Media media = this.media.get();
        return media.getSeries(seriesUUID)
            .map(series -> Pair.of(
                series.getName(),
                playlist(
                    PlaylistProvider.series,
                    series.getName(),
                    media.subLibrary(series).getAlbums())));
    }
    
    private Stream<Pair<String, Template>> playlistSublibrary(UUID playlistUUID) {
        Media media = this.media.get();
        return media.getPlaylist(playlistUUID)
            .map(playlist -> Pair.of(
                playlist.getName(),
                playlist(
                    PlaylistProvider.playlist,
                    playlist.getName(),
                    media.subLibrary(playlist).getAlbums())));
    }
    
    private Stream<Pair<String, Template>> curationSublibrary(UUID curationUUID) {
        Media media = this.media.get();
        return media.getCuration(curationUUID)
            .map(curation -> Pair.of(
                curation.getName(),
                playlist(
                    PlaylistProvider.curation,
                    curation.getName(),
                    media.subLibrary(curation).getAlbums())));
    }
    
    private Stream<Pair<String, Template>> artistPlaylist(UUID artistUUID) {
        Media media = this.media.get();
        return media.getArtist(artistUUID).flatMap(artist -> {
            Collection<Track> tracksFeaturing = media.getTracksFeaturing(artist).collect(Collectors.toList());
            if (tracksFeaturing.isEmpty()) {
                return Stream.empty();
            }
            Template playlist = playlist(artist, tracksFeaturing);
            Pair<String, Template> of = Pair.of(artist.getName(), playlist);
            return Stream.of(of);
        });
    }
    
    private Template playlist(Album album) {
        return baseTemplate(playlistM3u(album));
    }
    
    private PlaylistM3U playlistM3u(Album album) {
        return new PlaylistM3U(album.getArtist().getName() + ": " + album.getName(), album.getTracks());
    }
    
    private Template playlist(Artist artist, Collection<Track> tracks) {
        return baseTemplate(new PlaylistM3U(
            MessageFormat.format(
                "{0}: {1} ({2} tracks)",
                PlaylistProvider.artist, artist.getName(), tracks.size()),
            tracks));
    }
    
    private Template playlist(PlaylistProvider type, String name, Collection<Album> albums) {
        Collection<Track> tracks =
            albums.stream().map(Album::getTracks).flatMap(Collection::stream).collect(Collectors.toList());
        return baseTemplate(new PlaylistM3U(
            MessageFormat.format(
                "{0}: {1} ({2} albums, {3} tracks)",
                type, name, albums.size(), tracks.size()),
            tracks));
    }
    
    private Template baseTemplate(PlaylistM3U value) {
        return getTemplate(PLAYLIST_M3U).add(TPar.playlist, value);
    }
    
    private static final String AUDIO_X_MPEGURL = "audio/x-mpegurl";
    
    private static Headers setFilename(String template, UUID cookie) {
        return headers ->
            headers.set(
                HttpHeaderNames.CONTENT_DISPOSITION,
                MessageFormat.format("inline; filename=\"{0}-{1}-playlist.m3u\"",
                    template, Print.uuid(cookie)));
    }
}
