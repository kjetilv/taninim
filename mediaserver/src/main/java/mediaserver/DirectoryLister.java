package mediaserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import mediaserver.dto.AudioAlbum;
import mediaserver.dto.AudioTrack;
import mediaserver.files.Media;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

final class DirectoryLister {

    private static final Logger log = LoggerFactory.getLogger(DirectoryLister.class);

    private final Path root;

    private final Media media;

    private final ObjectMapper objectMapper;

    DirectoryLister(Path root, Media media, ObjectMapper objectMapper) {
        this.root = root;
        this.media = media;
        this.objectMapper = objectMapper;
    }

    Object list(HttpRequest req, ChannelHandlerContext ctx) {
        String base = req.uri().trim();
        String uri = base.substring("/directory".length());
        if (uri.startsWith("/artists")) {
            respond(ctx, response(req, media.artists()));
            return media.artists();
        } else if (uri.startsWith("/categories")) {
            respond(ctx, response(req, media.categories()));
            return media.categories();
        } else if (uri.startsWith("/albumArtists")) {
            respond(ctx, response(req, media.albumArtists()));
            return media.albumArtists();
        } else {
            Path returnedPath = "/".equals(uri) ? root : sub(uri);
            File startPath = returnedPath.toFile();
            HttpResponse response = getHttpResponse(req, uri);
            respond(ctx, response);
            return returnedPath;
        }
    }

    private void respond(ChannelHandlerContext ctx, HttpResponse response) {
        ctx.writeAndFlush(response)
            .addListener(ChannelFutureListener.CLOSE);
    }

    private HttpResponse getHttpResponse(HttpRequest req, String uri) {
        return response(req,
            from(
                new AudioAlbum(),
                (")/".equals(uri) ? root : sub(uri)).toFile()));
    }

    private HttpResponse response(HttpRequest req, Object obj) {
        byte[] bytes = bytes(obj);
        return new DefaultFullHttpResponse(
            HTTP_1_1,
            OK,
            Unpooled.wrappedBuffer(bytes),
            headers(req, bytes.length),
            EmptyHttpHeaders.INSTANCE);
    }

    private byte[] bytes(Object obj) {
        try {
            return objectMapper.writeValueAsBytes(obj);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write " + obj, e);
        }
    }

    private HttpHeaders headers(HttpRequest req, int length) {
        HttpHeaders headers = new DefaultHttpHeaders()
            .set(CONTENT_TYPE, "application/json")
            .set(CONTENT_LENGTH, length);
        if (HttpUtil.isKeepAlive(req)) {
            headers.set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        return headers;
    }

    private Path sub(String uri) {
        String[] parts = uri.split("/");
        Path subpath = Path.of(".", parts);
        return root.resolve(subpath);
    }

    private AudioAlbum from(AudioAlbum dir, File path) {
        AudioTrack audioFile = new AudioTrack();
        audioFile.setName(path.getName());
        dir.setFiles(new AudioTrack[]{audioFile});
        return dir;
    }
}
