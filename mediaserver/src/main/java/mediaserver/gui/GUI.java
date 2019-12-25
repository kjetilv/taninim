package mediaserver.gui;

import io.netty.channel.ChannelHandlerContext;
import mediaserver.http.*;
import mediaserver.media.*;
import mediaserver.sessions.Sessions;

import java.util.Optional;
import java.util.function.Supplier;

import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

public final class GUI extends TemplateEnabled {

    public static final String ID_COOKIE = "taninim-id";

    public static final String COOKIE_COOKIE = "taninim-cookies-ok";

    private final Supplier<Media> media;

    private final Sessions sessions;

    public GUI(Supplier<Media> media, Sessions sessions, Templater templater) {

        super(templater, Prefix.INDEX, Prefix.ALBUM);
        this.media = media;
        this.sessions = sessions;
    }

    @Override
    public Handling handleRequest(WebPath webPath, ChannelHandlerContext ctx) {

        return template(webPath, media.get())
            .map(template ->
                respond(webPath, ctx, template))
            .orElseGet(() ->
                handle(ctx, NOT_FOUND));
    }

    private Optional<Template> template(WebPath webPath, Media media) {

        QPars pars = webPath.getQueryParameters();

        if (webPath.hasPrefix(LOGIN)) {
            return Optional.of(login());
        }

        if (webPath.hasPrefix(ALBUM)) {
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

        return Optional.of(indexTemplate()
            .add(QPar.USER, sessions.activeUser(webPath))
            .add(QPar.MEDIA, submedia)
            .add(QPar.ARTIST, artist));
    }

    private Template album(WebPath webPath, Media media, Album album, QPars pars) {

        Optional<Track> track = pars.apply(QPar.TRACK).flatMap(media::getTrack);

        return albumTemplate()
            .add(QPar.USER, sessions.activeUser(webPath))
            .add(QPar.MEDIA, media)
            .add(QPar.ALBUM, album)
            .add(QPar.PLAY_TRACK, track.orElse(null))
            .add(QPar.PLAY_TRACKS, album.getTracks());
    }
}
