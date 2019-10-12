package mediaserver;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import mediaserver.files.Album;
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

public class GUI extends Nettish {

    private final Media media;

    private static final String TEXT_HTML = "text/html";

    public GUI(IO io, Media media) {
        super(io, "/");
        this.media = media;
    }

    @Override
    HttpResponse handle(HttpRequest req, String uri, ChannelHandlerContext ctx) {
        Template template = template(uri, this.media);
        return respond(
            ctx,
            response(req, TEXT_HTML, template.bytes()));
    }

    private Template template(String uri, Media media) {
        int queryParamPos = uri.indexOf("?");
        String uriPath = uriPath(uri, queryParamPos);
        Map<String, String> pars = params(uri, queryParamPos);
        return uuid(pars, "album").flatMap(media::getAlbum)
            .map(album ->
                albumTemplate(media, album, pars))
            .orElseGet(() ->
                indexTemplate(media, uriPath, pars));
    }

    private Template indexTemplate(Media media, String uriPath, Map<String, String> pars) {
        CategoryPath categoryPath = getCategoryPath(uriPath);
        return template("index.html")
            .add("media", media.subLibrary(categoryPath))
            .add("artist", pars.get("artist"))
            .add("album", pars.get("album"));
    }

    private Template albumTemplate(Media media, Album album, Map<String, String> pars) {
        Template tmpl = template("album.html")
            .add("media", media)
            .add("album", album);
        return uuid(pars, "track").flatMap(media::getTrack)
            .map(playTrack ->
                tmpl.add("playTrack", playTrack))
            .orElse(tmpl);
    }

    private static Optional<UUID> uuid(Map<String, String> pars, String name) {
        try {
            return Optional.ofNullable(pars.get(name)).map(UUID::fromString);
        } catch (Exception e) {
            throw new IllegalStateException("Not av valid album", e);
        }
    }

    private static String uriPath(String uri, int queryParamPos) {
        return queryParamPos < 0 ? uri : uri.substring(0, uri.indexOf("?"));
    }

    private static Map<String, String> params(String uri, int queryParamPos) {
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
}
