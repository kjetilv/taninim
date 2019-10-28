package mediaserver.gui;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import mediaserver.files.Album;
import mediaserver.files.Artist;
import mediaserver.files.CategoryPath;
import mediaserver.files.Media;
import mediaserver.util.IO;
import mediaserver.util.URLs;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public class GUI extends Nettish {

    private final Media media;

    private static final String TEXT_HTML = "text/html";

    public GUI(IO io, Media media) {

        super(io, "/");
        this.media = media;
    }

    @Override
    public HttpResponse handle(HttpRequest req, String path, ChannelHandlerContext ctx) {

        Template template = template(resource(path), this.media);
        return respond(
            ctx,
            path,
            response(req, TEXT_HTML, template.bytes()));
    }

    private Template template(String uri, Media media) {

        int queryParamPos = uri.indexOf("?");
        String uriPath = uriPath(uri, queryParamPos);
        QPars pars = new QPars(params(uri, queryParamPos));
        return pars.apply(QPar.ALBUM).flatMap(media::getAlbum)
            .map(album ->
                     albumTemplate(media, album, pars))
            .orElseGet(() ->
                           indexTemplate(media, uriPath, pars));
    }

    private Template indexTemplate(Media media, String uriPath, QPars pars) {

        CategoryPath categoryPath = getCategoryPath(uriPath);
        Artist artist = pars.apply(QPar.ARTIST).flatMap(media::getArtist).orElse(null);
        return template("index.html")
            .add(QPar.MEDIA, media.subLibrary(categoryPath, artist))
            .add(QPar.ARTIST, pars.apply(QPar.ARTIST).orElse(null))
            .add(QPar.ALBUM, pars.apply(QPar.ARTIST).orElse(null));
    }

    private Template albumTemplate(Media media, Album album, QPars pars) {

        Template tmpl = template("album.html")
            .add(QPar.MEDIA, media)
            .add(QPar.ALBUM, album);
        return pars.apply(QPar.TRACK).flatMap(media::getTrack)
            .map(playTrack ->
                     tmpl.add(QPar.PLAY_TRACK, playTrack))
            .orElse(tmpl);
    }

    private static String uriPath(String uri, int queryParamPos) {

        return queryParamPos < 0 ? uri : uri.substring(0, uri.indexOf("?"));
    }

    private static Map<QPar, String> params(String uri, int queryParamPos) {

        return queryParamPos < 0
            ? Collections.emptyMap()
            : URLs.queryParams(uri.substring(queryParamPos + 1));
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

    private class QPars implements Function<QPar, Optional<UUID>> {

        private final Map<QPar, String> pars;

        private QPars(Map<QPar, String> pars) {

            this.pars = pars;
        }

        @Override
        public Optional<UUID> apply(QPar par) {

            Optional<String> value = Optional.ofNullable(pars.get(par));
            try {
                return value.map(UUID::fromString);
            } catch (Exception e) {
                throw new IllegalStateException("Invalid album: " + value.get(), e);
            }
        }
    }
}
