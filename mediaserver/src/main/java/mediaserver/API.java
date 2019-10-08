package mediaserver;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    public boolean handle(HttpRequest req, String path, ChannelHandlerContext ctx) {
        if (path.startsWith("/albums")) {
            Collection<AudioAlbum> albums = audioAlbums(media.getAlbums());
            return respond(ctx, response(req, albums));
        }
        if (path.startsWith("/artists")) {
            return respond(ctx, response(req, media.getArtists()));
        }
        if (path.startsWith("/categories")) {
            return respond(ctx, response(req, media.getCategories()));
        }
        if (path.startsWith("/albumArtists")) {
            return respond(ctx, response(req, media.getAlbumArtists()));
        }
        return false;
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
        return response(req, bytes(obj));
    }

    private byte[] bytes(Object obj) {
        try {
            return objectMapper.writeValueAsBytes(obj);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write " + obj, e);
        }
    }
}
