package mediaserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import mediaserver.dto.AudioAlbum;
import mediaserver.dto.AudioTrack;
import mediaserver.files.Album;
import mediaserver.files.Media;
import mediaserver.files.Track;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.stream.Collectors;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpResponseStatus.ACCEPTED;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

final class API extends Nettish {

    private static final Logger log = LoggerFactory.getLogger(API.class);

    private final Media media;

    private final ObjectMapper objectMapper;

    API(Media media, ObjectMapper objectMapper) {
        super("/api");
        this.media = media;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpRequest req, String path, ChannelHandlerContext ctx) {
        if (path.startsWith("/albums")) {
            Collection<AudioAlbum> albums = audioAlbums(media.getAlbums());
            respond(ctx, response(req, albums));
        } else if (path.startsWith("/artists")) {
            respond(ctx, response(req, media.getArtists()));
        } else if (path.startsWith("/categories")) {
            respond(ctx, response(req, media.getCategories()));
        } else if (path.startsWith("/albumArtists")) {
            respond(ctx, response(req, media.getAlbumArtists()));
        } else {
            respond(ctx, new DefaultFullHttpResponse(HTTP_1_1, ACCEPTED));
        }
    }

    private Collection<AudioAlbum> audioAlbums(Collection<Album> albums) {
        return albums.stream()
            .map(this::audioAlbum)
            .collect(Collectors.toList());
    }

    private AudioAlbum audioAlbum(Album a) {
        AudioAlbum aa = new AudioAlbum();
        aa.setArtist(a.getArtist());
        aa.setName(a.getName());
        aa.setUuid(a.getUuid().toString());
        aa.setTracks(audioTracks(a));
        return aa;
    }

    private Collection<AudioTrack> audioTracks(Album a) {
        return a.getTracks().stream()
            .map(this::audioTrack)
            .collect(Collectors.toList());
    }

    private AudioTrack audioTrack(Track t) {
        AudioTrack at = new AudioTrack();
        at.setName(t.getName());
        at.setUuid(t.getUuid().toString());
        return at;
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
            .set(CONTENT_LENGTH, length)
            .set(ACCESS_CONTROL_ALLOW_HEADERS, "*");
        if (HttpUtil.isKeepAlive(req)) {
            headers.set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        return headers;
    }

}
