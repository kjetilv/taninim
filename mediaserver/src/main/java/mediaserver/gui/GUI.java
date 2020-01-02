package mediaserver.gui;

import mediaserver.http.*;
import mediaserver.media.*;

import java.util.Optional;
import java.util.function.Supplier;

public final class GUI extends TemplateEnabled {

    public static final String ID_COOKIE = "taninim-id";

    public static final String COOKIE_COOKIE = "taninim-cookies-ok";

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
                .map(album ->
                    album(webPath, media, album, pars))
                .or(() ->
                    index(webPath, media, pars));
        }

        return index(webPath, media, pars);
    }

    private Optional<Template> index(WebPath webPath, Media media, QPars pars) {

        Artist artist =
            pars.apply(QPar.ARTIST).flatMap(media::getArtist).orElse(null);
        Series series =
            pars.apply(QPar.SERIES).flatMap(media::getSeries).orElse(null);
        Playlist playlist =
            pars.apply(QPar.PLAYLIST).flatMap(media::getPlaylist).orElse(null);

        Media submedia = media.subLibrary(null, artist, series, playlist);
        if (submedia.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(base(indexTemplate(), webPath, submedia)
            .add(QPar.ARTIST, artist)
            .add(QPar.SERIES, series)
            .add(QPar.PLAYLIST, playlist));
    }

    private Template album(WebPath webPath, Media media, Album album, QPars pars) {

        Optional<Track> track = pars.apply(QPar.TRACK).flatMap(media::getTrack);
        return base(albumTemplate(), webPath, media)
            .add(QPar.ALBUM, album)
            .add(QPar.PLAYLISTS, Playlist.playlistsWith(album))
            .add(QPar.PLAY_TRACK, track.orElse(null))
            .add(QPar.PLAY_TRACKS, album.getTracks());
    }

    private Template base(Template template, WebPath webPath, Media media) {

        return template
            .add(QPar.USER, webPath.getSession().getActiveUser())
            .add(QPar.MEDIA, media);
    }
}
