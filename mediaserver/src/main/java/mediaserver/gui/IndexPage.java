package mediaserver.gui;

import mediaserver.Config;
import mediaserver.Globals;
import mediaserver.http.*;
import mediaserver.media.*;
import mediaserver.templates.TPar;
import mediaserver.templates.Template;
import mediaserver.toolkit.Templater;
import mediaserver.util.P2;
import mediaserver.util.R;

import java.util.Optional;
import java.util.function.Supplier;

public final class IndexPage extends TemplateEnabled {

    public static final String ID_COOKIE = "taninim-id";

    private final Supplier<Media> media;

    public IndexPage(Supplier<Media> media, Templater templater) {

        super(templater, Page.INDEX);
        this.media = media;
    }

    public static Optional<P2<Album, Track>> resolveRandomTrack(Req req, Media submedia) {

        return req.getSession().getRandomTrack(req.getTime(), () -> randomAlbumTrack(submedia));
    }

    @Override
    protected Handling handleRequest(Req req) {

        return template(req, media.get())
            .map(template ->
                respondHtml(req, template))
            .orElseGet(() ->
                handleNotFound(req));
    }

    private Optional<Template> template(Req req, Media media) {

        QPars pars = req.getQueryParameters();

        Artist artist =
            pars.get(QPar.ARTIST).flatMap(media::getArtist).orElse(null);
        Series series =
            pars.get(QPar.SERIES).flatMap(media::getSeries).orElse(null);
        Playlist curation =
            pars.get(QPar.CURATION).flatMap(media::getCuration).orElse(null);
        Playlist playlist = curation == null
            ? pars.get(QPar.PLAYLIST).flatMap(media::getPlaylist).orElse(null)
            : null;

        return Optional.ofNullable(
            media.subLibrary(null, artist, series, curation == null ? playlist : curation))
            .filter(submedia -> !submedia.isEmpty())
            .map(submedia -> {

                Optional<P2<Album, Track>> globalTrack = Globals.globalTrack(req);
                Optional<P2<Album, Track>> accessibleTrack = globalTrack.or(() -> resolveRandomTrack(req, submedia));

                return getTemplate(INDEX_PAGE)
                    .add(TPar.PLYR, Config.PLYR)
                    .add(TPar.HIGHLIGHTED_ALBUM, accessibleTrack.map(P2::getT1))
                    .add(TPar.HIGHLIGHTED, accessibleTrack.map(P2::getT2))
                    .add(TPar.HIGHLIGHTED_SELECTED, globalTrack.isPresent())
                    .add(TPar.RANDOM_ALBUMS, submedia.getRandomAlbums(7))
                    .add(TPar.USER, req.getSession().getActiveUser(req))
                    .add(TPar.MEDIA, submedia)
                    .add(TPar.ARTIST, artist)
                    .add(TPar.SERIES, series)
                    .add(TPar.PLAYLIST, playlist)
                    .add(TPar.CURATION, curation);
            });
    }

    private static Optional<P2<Album, Track>> randomAlbumTrack(Media submedia) {

        return R.nd(submedia.getRandomAlbums(20))
            .flatMap(album ->
                R.nd(album.getTracks())
                    .map(track ->
                        new P2<>(album, track)));
    }
}
