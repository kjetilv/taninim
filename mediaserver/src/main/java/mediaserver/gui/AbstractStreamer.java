package mediaserver.gui;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import mediaserver.http.*;
import mediaserver.media.Media;
import mediaserver.media.Track;
import mediaserver.sessions.AccessLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.LongStream;

import static io.netty.channel.ChannelFutureListener.CLOSE;
import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.BYTES;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.handler.codec.http.LastHttpContent.EMPTY_LAST_CONTENT;

public abstract class AbstractStreamer extends NettyHandler implements Streamer {

    private static final Logger log = LoggerFactory.getLogger(AbstractStreamer.class);

    protected final Supplier<Media> media;

    private final int bytesPerChunk;

    AbstractStreamer(Supplier<Media> media, int bytesPerChunk) {

        super(Page.AUDIO);
        this.media = media;
        this.bytesPerChunk = bytesPerChunk;
    }

    @Override
    public Handling handleRequest(WebPath webPath) {

        if (webPath.getSession().hasLevel(AccessLevel.STREAM_CURATED)) {
            return getMediaTrack(webPath.getPath(true))
                .filter(track ->
                    Streamer.isAuthorized(webPath, track, this.media))
                .map(track ->
                    stream(webPath, track))
                .map(response ->
                    handled(webPath, response))
                .orElseGet(() ->
                    handleNotFound(webPath));
        }
        return handleUnauthorized(webPath);
    }

    @Override
    public HttpResponse stream(WebPath webPath, Track track) {

        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
        boolean lossless = webPath.isFlac() && webPath.getSession().isPrivileged();
        response.headers().set(CONTENT_TYPE, lossless
            ? WebPath.AUDIO_FLAC
            : WebPath.AUDIO_AAC);
        if (webPath.isKeepAlive()) {
            response.headers().set(CONNECTION, KEEP_ALIVE);
        }
        response.headers().add(ACCEPT_RANGES, BYTES);
        if (webPath.getRequest().method() == HttpMethod.GET) {
            return streamFuture(webPath, track, lossless, response)
                .map(listen(webPath, track))
                .map(close(webPath, response))
                .orElseGet(() ->
                    Netty.respondRaw(webPath.getCtx(), REQUESTED_RANGE_NOT_SATISFIABLE));
        }
        if (webPath.getRequest().method() == HttpMethod.HEAD) {
            response.setStatus(OK);
            response.headers().set(CONTENT_LENGTH, length(track, lossless));
            return Netty.respond(webPath.getCtx(), response);
        }
        return Netty.respondRaw(webPath.getCtx(), BAD_REQUEST);
    }

    public Function<ChannelFuture, HttpResponse> close(WebPath webPath, HttpResponse response) {

        return future -> {
            if (!webPath.isKeepAlive()) {
                webPath.getCtx().writeAndFlush(EMPTY_LAST_CONTENT).addListener(CLOSE);
            }
            return response;
        };
    }

    protected Chunk chunk(BytesRange rangeHeader, long fileLength) {

        try {
            long start = rangeHeader.getStart();
            long rangeExclusiveEnd = rangeHeader.getEndExclusive(fileLength);
            long exclusiveEnd = min(rangeExclusiveEnd, start + fileLength, start + bytesPerChunk);

            return new Chunk(start, exclusiveEnd, fileLength);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid range header", e);
        }
    }

    protected abstract Optional<ChannelFuture> streamFuture(
        WebPath webPath,
        Track track,
        boolean lossless,
        HttpResponse response
    );

    protected ChannelFuture respondData(
        WebPath webPath,
        HttpResponse response,
        Chunk chunk,
        Object content
    ) {

        try {
            response.headers().set(CONTENT_LENGTH, chunk.getSize());
            response.headers().set(CONTENT_RANGE, chunk.range());
            response.setStatus(PARTIAL_CONTENT);
            try {
                ChannelFuture header = webPath.getCtx().write(response);
                if (chunk.getSize() > 0) {
                    return webPath.getCtx().write(content, webPath.getCtx().newProgressivePromise());
                }
                log.info("Responding with {} => {}", chunk, webPath);
                return header;
            } finally {
                webPath.getCtx().flush();
            }
        } finally {
            webPath.getSession().streaming(chunk.getSize());
        }
    }

    protected abstract long length(Track track, boolean lossless);

    private Function<ChannelFuture, ChannelFuture> listen(WebPath webPath, Track track) {

        return channelFuture -> {
            if (channelFuture instanceof ChannelProgressiveFuture) {
                ((ChannelProgressiveFuture) channelFuture).addListener(
                    new ProgressListener(String.format("%s %s", webPath.getSession(), track))
                );
            }
            return channelFuture;
        };
    }

    private long min(long... lengths) {

        return LongStream.of(lengths).min().orElseThrow(() ->
            new IllegalStateException("No end in sight! " + Arrays.toString(lengths)));
    }

    private Optional<Track> getMediaTrack(String path) {

        return media.get().getTrack(pathUUID(path));
    }

    private UUID pathUUID(String path) {

        try {
            int typeIndex = path.indexOf('.', 1);
            String uuidPart = typeIndex < 0 ? path : path.substring(0, typeIndex);
            return UUID.fromString(uuidPart);
        } catch (Exception e) {
            throw new IllegalArgumentException("Ugyldig UUID: " + path, e);
        }
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + media + "]";
    }
}
