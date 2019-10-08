package mediaserver;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import mediaserver.files.Album;
import mediaserver.files.CategoryPath;
import mediaserver.files.Media;
import org.stringtemplate.v4.ST;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class GUI extends Nettish {

    private final Media media;

    private static final String TEXT_HTML = "text/html";

    public GUI(Media media) {
        super("/");
        this.media = media;
    }

    @Override
    boolean handle(HttpRequest req, String uri, ChannelHandlerContext ctx) {
        ST html = template(uri, this.media);
        respondHtml(ctx, html.render());
        return true;
    }

    private void respondHtml(ChannelHandlerContext ctx, String html) {
        respond(ctx, response(html));
    }

    private static ST template(String uri, Media media) {
        int queryParamPos = uri.indexOf("?");
        String uriPath = uriPath(uri, queryParamPos);
        Map<String, String> pars = params(uri, queryParamPos);
        return uuid(pars, "album").flatMap(media::getAlbum)
            .map(album ->
                albumTemplate(media, album, pars))
            .orElseGet(() ->
                indexTemplate(media, uriPath, pars));
    }

    private static ST indexTemplate(Media media, String uriPath, Map<String, String> pars) {
        CategoryPath categoryPath = getCategoryPath(uriPath);
        ST st = newTemplate("index.html");
        st.add("media", media.subLibrary(categoryPath));
        st.add("artist", pars.get("artist"));
        st.add("album", pars.get("album"));
        return st;
    }

    private static ST albumTemplate(Media media, Album album, Map<String, String> pars) {
        ST st = newTemplate("album.html");
        st.add("media", media);
        st.add("album", album);
        uuid(pars, "track").flatMap(media::getSong).ifPresent(playTrack -> {
            st.add("playTrack", playTrack);
            album.nextTrack(playTrack).ifPresent(next -> {
                st.add("nextTrack", next);
            });
        });
        return st;
    }

    private static Optional<UUID> uuid(Map<String, String> pars, String name) {
        try {
            return Optional.ofNullable(pars.get(name)).map(UUID::fromString);
        } catch (Exception e) {
            throw new IllegalStateException("Not av valid album", e);
        }
    }

    private static DefaultFullHttpResponse response(String html) {
        return new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK,
            Unpooled.wrappedBuffer(html.getBytes(StandardCharsets.UTF_8)),
            new DefaultHttpHeaders()
                .set(HttpHeaderNames.CONTENT_TYPE, TEXT_HTML),
            EmptyHttpHeaders.INSTANCE);
    }

    private static String uriPath(String uri, int queryParamPos) {
        return queryParamPos < 0 ? uri : uri.substring(0, uri.indexOf("?"));
    }

    private static Map<String, String> params(String uri, int queryParamPos) {
        return queryParamPos < 0
            ? Collections.emptyMap()
            : IO.queryParams(uri.substring(queryParamPos + 1));
    }

    private static Path path(String uri) {
        String[] split = uri.split("/");
        String[] tail = new String[split.length - 1];
        System.arraycopy(split, 1, tail, 0, tail.length);
        return Paths.get(split[0], tail);
    }

    private static ST newTemplate(String resource) {
        return IO.read(resource).map(data ->
            new ST(data, '{', '}')
        ).orElseThrow(() ->
            new IllegalArgumentException("Invalid template: " + resource));
    }

    private static CategoryPath getCategoryPath(String uriPath) {
        return uriPath == null || uriPath.isBlank()
            ? CategoryPath.ROOT
            : new CategoryPath(path(uriPath));
    }
}
