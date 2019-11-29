package mediaserver.gui;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import mediaserver.Media;
import mediaserver.externals.FacebookUser;
import mediaserver.files.*;
import mediaserver.sessions.Sessions;
import mediaserver.util.IO;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Supplier;

public class GUI extends Nettish {

    public static final String ID_COOKIE = "taninim-id";

    public static final String COOKIE_COOKIE = "taninim-cookies-ok";

    public static final String ALBUM = "album";

    private final Supplier<Media> media;

    private final Sessions sessions;

    private static final String TEXT_HTML = "text/html";

    public GUI(Supplier<Media> media, Sessions sessions) {

        super("/");
        this.media = media;
        this.sessions = sessions;
    }

    @Override
    public HttpResponse handle(FullHttpRequest req, String path, ChannelHandlerContext ctx) {

        return template(req, resource(path), media.get())
            .map(Template::bytes)
            .map(bytes ->
                response(req, null, TEXT_HTML, bytes, null))
            .map(response ->
                respond(ctx, response))
            .orElseGet(() ->
                super.handle(req, path, ctx));
    }

    private Optional<Template> template(HttpRequest req, String uri, Media media) {

        QPars pars = qpars(uri);
        String uriPath = uriPath(uri);

        if (ALBUM.equals(unslashed(uriPath))) {
            return pars.apply(QPar.ALBUM)
                .flatMap(media::getAlbum)
                .map(album ->
                    albumTemplate(req, media, album, pars))
                .or(() ->
                    indexTemplate(req, media, uriPath, pars));
        }
        return indexTemplate(req, media, uriPath, pars);
    }

    private String unslashed(String uriPath) {

        return uriPath.startsWith("/") ? unslashed(uriPath.substring(1))
            : uriPath.endsWith("/") ? unslashed(uriPath.substring(0, uriPath.length() - 1))
            : uriPath.toLowerCase();
    }

    private Optional<Template> indexTemplate(
        HttpRequest req,
        Media media,
        String uriPath,
        QPars pars
    ) {

        CategoryPath categoryPath = getCategoryPath(uriPath);
        Artist artist =
            pars.apply(QPar.ARTIST).flatMap(media::getArtist).orElse(null);
        Series series =
            pars.apply(QPar.SERIES).flatMap(media::getSeries).orElse(null);
        Media submedia = media.subLibrary(categoryPath, artist, series);
        if (submedia.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(initTemplate(req, "res/index.html")
            .add(QPar.MEDIA, submedia)
            .add(QPar.ARTIST, artist));
    }

    private Template albumTemplate(HttpRequest req, Media media, Album album, QPars pars) {

        Optional<Track> track = pars.apply(QPar.TRACK).flatMap(media::getTrack);
        return initTemplate(req, "res/album.html")
            .add(QPar.MEDIA, media)
            .add(QPar.ALBUM, album)
            .add(QPar.PLAY_TRACK, track.orElse(null))
            .add(QPar.PLAY_TRACKS, album.getTracks());
    }

    private Template initTemplate(HttpRequest req, String resource) {

        Optional<FacebookUser> userName = sessions.activeUser(req);
        return template(resource)
            .add(QPar.USER, userName.orElse(null));
    }

    private static Path path(String uri) {

        String[] split = uri.split("/");
        String[] tail = new String[split.length - 1];
        System.arraycopy(split, 1, tail, 0, tail.length);
        return Paths.get(split[0], tail);
    }

    private static CategoryPath getCategoryPath(String uriPath) {

        return uriPath == null || uriPath.isBlank()
            ? CategoryPath.ROOT
            : new CategoryPath(path(uriPath));
    }
}
