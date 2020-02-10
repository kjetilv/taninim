package mediaserver.gui;

import mediaserver.Config;
import mediaserver.Globals;
import mediaserver.http.*;
import mediaserver.media.*;
import mediaserver.sessions.AccessLevel;
import mediaserver.templates.TPar;
import mediaserver.templates.Template;
import mediaserver.toolkit.Templater;
import mediaserver.util.Pair;
import mediaserver.util.Print;
import mediaserver.util.R;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

public final class IndexPage extends TemplateEnabled {

    public static final String ID_COOKIE = "taninim-id";

    private final Supplier<Media> media;

    public IndexPage(Supplier<Media> media, Templater templater) {

        super(templater, Route.INDEX);
        this.media = media;
    }

    public static Optional<Pair<Album, Track>> resolveRandomTrack(Req req, Media submedia) {

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
            pars.get(QPar.artist).flatMap(media::getArtist).orElse(null);
        Series series =
            pars.get(QPar.series).flatMap(media::getSeries).orElse(null);
        Playlist curation =
            pars.get(QPar.curation).flatMap(media::getCuration).orElse(null);
        Playlist playlist = curation == null
            ? pars.get(QPar.playlist).flatMap(media::getPlaylist).orElse(null)
            : null;

        return Optional.ofNullable(
            media.subLibrary(null, artist, series, curation == null ? playlist : curation))
            .filter(submedia -> !submedia.isEmpty())
            .map(submedia -> {

                boolean streamSingle = req.getSession().getAccessLevel().satisfies(AccessLevel.STREAM_SINGLE);
                Optional<Pair<Album, Track>> globalTrack =
                    streamSingle ? Globals.get().getGlobalTrack(req.getTime()) : Optional.empty();
                Optional<Pair<Album, Track>> accessible =
                    streamSingle ? globalTrack.or(() -> resolveRandomTrack(req, submedia)) : Optional.empty();

                Optional<Album> accessibleAlbum = accessible.map(Pair::getT1);
                Optional<Track> accessibleTrack = accessible.map(Pair::getT2);

                return getTemplate(INDEX_PAGE)
                    .add(TPar.plyr, Config.PLYR)
                    .add(TPar.highlightedAlbum, accessibleAlbum)
                    .add(TPar.highlighted, accessibleTrack)
                    .add(TPar.highlightedSelected, globalTrack.isPresent())
                    .add(
                        TPar.highlightedRemaining,
                        remainingTime(req, globalTrack, accessibleTrack).map(Print::prettyLongTime))
                    .add(TPar.randomAlbums, submedia.getRandomAlbums(7))
                    .add(TPar.user, req.getSession().getActiveUser(req))
                    .add(TPar.media, submedia)
                    .add(TPar.artist, artist)
                    .add(TPar.series, series)
                    .add(TPar.playlist, playlist)
                    .add(TPar.curation, curation);
            });
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static Optional<Duration> remainingTime(
        Req req,
        Optional<Pair<Album, Track>> globalTrack,
        Optional<Track> accessibleTrack
    ) {

        if (accessibleTrack.isEmpty()) {
            return Optional.empty();
        }
        if (globalTrack.isPresent()) {
            return Globals.get().getGlobalTrackRemaining(req.getTime());
        }
        return req.getSession().getRandomTrackRemaining(req.getTime());
    }

    private static Optional<Pair<Album, Track>> randomAlbumTrack(Media submedia) {

        return R.nd(submedia.getRandomAlbums(20))
            .flatMap(album ->
                R.nd(album.getTracks())
                    .map(track ->
                        Pair.of(album, track)));
    }
}
