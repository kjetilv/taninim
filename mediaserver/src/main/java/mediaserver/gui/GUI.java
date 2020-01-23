package mediaserver.gui;

import mediaserver.Config;
import mediaserver.http.*;
import mediaserver.media.*;
import mediaserver.sessions.AccessLevel;
import mediaserver.sessions.Session;
import mediaserver.toolkit.Templater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class GUI extends TemplateEnabled {

    public static final String ID_COOKIE = "taninim-id";

    public static final String COOKIE_COOKIE = "taninim-cookies-ok";

    private static final Logger log = LoggerFactory.getLogger(GUI.class);

    private final Supplier<Media> media;

    public GUI(Supplier<Media> media, Templater templater) {

        super(templater, Page.INDEX, Page.ALBUM);
        this.media = media;
    }

    @Override
    public Handling handleRequest(WebPath webPath) {

        return template(webPath, media.get())
            .map(template ->
                respondHtml(webPath, template))
            .orElseGet(() ->
                handleNotFound(webPath));
    }

    private Optional<Template> template(WebPath webPath, Media media) {

        QPars pars = webPath.getQueryParameters();
        if (webPath.isFor(Page.ALBUM)) {
            return pars.apply(QPar.ALBUM)
                .flatMap(media::getAlbum)
                .map(album -> albumTemplate(
                    webPath,
                    media,
                    album,
                    playTrack(webPath, media, pars),
                    playTracks(webPath, media, album)))
                .or(() -> {
                    log.warn("Was asked for {}", pars.apply(QPar.ALBUM)
                        .map(unknown -> "unknown album " + unknown)
                        .orElse("unspecified album"));
                    return indexTemplate(webPath, media, pars);
                });
        }
        return indexTemplate(webPath, media, pars);
    }

    private Collection<Track> playTracks(WebPath webPath, Media media, Album album) {

        return album.getTracks().stream()
            .filter(authorized(media, webPath))
            .collect(Collectors.toList());
    }

    private Predicate<Track> authorized(Media media, WebPath webPath) {

        return track ->
            Streamer.isAuthorized(webPath, track, media);
    }

    private Track playTrack(WebPath webPath, Media media, QPars pars) {

        return pars.apply(QPar.TRACK)
            .flatMap(media::getTrack)
            .filter(authorized(media, webPath))
            .orElse(null);
    }

    private Optional<Template> indexTemplate(WebPath webPath, Media media, QPars pars) {

        Artist artist =
            pars.apply(QPar.ARTIST).flatMap(media::getArtist).orElse(null);
        Series series =
            pars.apply(QPar.SERIES).flatMap(media::getSeries).orElse(null);
        Playlist curation =
            pars.apply(QPar.CURATION).flatMap(media::getCuration).orElse(null);
        Playlist playlist = curation == null
            ? pars.apply(QPar.PLAYLIST).flatMap(media::getPlaylist).orElse(null)
            : null;

        Media submedia = media.subLibrary(null, artist, series, curation == null ? playlist : curation);
        if (submedia.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(base(getTemplate(INDEX_PAGE), webPath, submedia)
            .add(QPar.ARTIST, artist)
            .add(QPar.SERIES, series)
            .add(QPar.PLAYLIST, playlist)
            .add(QPar.CURATION, curation));
    }

    private Template albumTemplate(
        WebPath webPath,
        Media media,
        Album album,
        Track track,
        Collection<Track> playableTracks
    ) {

        Template template = base(getTemplate(ALBUM_PAGE), webPath, media)
            .add(QPar.ALBUM, album)
            .add(QPar.PLAYLISTS, Playlist.playlistsWith(album))
            .add(QPar.PLAY_TRACK, track)
            .add(QPar.PLAY_TRACKS, playableTracks);
        Session session = webPath.getSession();
        return session.hasLevel(AccessLevel.STREAM_CURATED) && ! session.hasLevel(AccessLevel.STREAM)
            ? template.add(QPar.CURATIONS, Playlist.curationsWith(album))
            : template;
    }

    private Template base(Template template, WebPath webPath, Media media) {

        return template
            .add(QPar.USER, webPath.getSession().getActiveUser(webPath))
            .add(QPar.MEDIA, media)
            .add(QPar.PLYR, Config.PLYR);
    }
}
