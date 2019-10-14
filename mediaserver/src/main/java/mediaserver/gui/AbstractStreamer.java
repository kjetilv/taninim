package mediaserver.gui;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import mediaserver.files.Media;
import mediaserver.files.Track;
import mediaserver.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public abstract class AbstractStreamer extends Nettish {

    private static final Logger log = LoggerFactory.getLogger(AbstractStreamer.class);

    private static final String AUDIO_FLAC = "audio/flac";

    protected final Media media;

    AbstractStreamer(Media media, IO io, String... prefix) {
        super(io, prefix);
        this.media = media;
    }

    @Override
    public HttpResponse handle(HttpRequest req, String path, ChannelHandlerContext ctx) {
        return getMediaTrack(resource(path))
            .map(track ->
                stream(req, track, ctx))
            .orElseGet(() ->
                respond(ctx, BAD_REQUEST));
    }

    static void updateHeaders(HttpResponse response, PartialRequestInfo pri, long length) {
        response.headers().add(HttpHeaderNames.CONTENT_RANGE,
            HttpHeaderValues.BYTES + " " + pri.getStartOffset() + "-" + pri.getEndOffset() + "/" + length);

        log.debug("{}: {}", HttpHeaderNames.CONTENT_RANGE, response.headers().get(HttpHeaderNames.CONTENT_RANGE));

        HttpUtil.setContentLength(response, pri.getChunkSize());
        log.debug("{}: {}", HttpHeaderNames.CONTENT_LENGTH, pri.getChunkSize());

        response.setStatus(HttpResponseStatus.PARTIAL_CONTENT);
    }

    protected static HttpResponse response(HttpRequest req) {
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
        response.headers().set(CONTENT_TYPE, AUDIO_FLAC);
        if (HttpUtil.isKeepAlive(req)) {
            response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        response.headers().add(HttpHeaderNames.ACCEPT_RANGES, HttpHeaderValues.BYTES);
        return response;
    }

    protected static PartialRequestInfo getPartialRequestInfo(String rangeHeader, long fileLength) {
        try {
            long startOffset = Integer.parseInt(rangeHeader.trim()
                .replace(HttpHeaderValues.BYTES + "=", "")
                .replace("-", ""));
            long endOffset = endOffset(fileLength, startOffset);
            long chunkSize = endOffset - startOffset + 1;
            return new PartialRequestInfo(startOffset, endOffset, chunkSize);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid range header", e);
        }
    }

    protected abstract HttpResponse stream(HttpRequest req, Track track, ChannelHandlerContext ctx);

    private static long endOffset(long fileLength, long startOffset) {
        long endOffset = startOffset + fileLength;
        return endOffset < fileLength ? endOffset : fileLength - 1;
    }

    private Optional<Track> getMediaTrack(String path) {
        return media.getTrack(UUID.fromString(path.substring(1)));
    }
}
