package mediaserver.gui;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import mediaserver.http.*;
import mediaserver.media.Media;
import mediaserver.media.Track;
import mediaserver.sessions.AccessLevel;
import mediaserver.toolkit.BytesRange;
import mediaserver.toolkit.Chunk;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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

    protected final Supplier<Media> media;

    private final int bytesPerChunk;

    private final Map<Track, Long> shortLengths = new ConcurrentHashMap<>();

    private final Map<Track, Long> longLengths = new ConcurrentHashMap<>();

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
                    authorized(webPath, track))
                .map(track ->
                    stream(webPath, track))
                .map(response ->
                    handled(webPath, response))
                .orElseGet(() ->
                    handleNotFound(webPath));
        }
        return handleBadRequest(webPath);
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + media + "]";
    }

    public boolean authorized(WebPath webPath, Track track) {

        return Streamer.isAuthorized(webPath, track, this.media);
    }

    @Override
    public HttpResponse stream(WebPath webPath, Track track) {

        HttpMethod method = webPath.getRequest().method();
        if (method == HttpMethod.HEAD || method == HttpMethod.GET) {
            return stream(webPath, track, method);
        } else {
            return Netty.respond(webPath.getCtx(), BAD_REQUEST);
        }

    }

    public HttpResponse stream(WebPath webPath, Track track, HttpMethod method) {
        ChannelHandlerContext ctx = webPath.getCtx();

        boolean lossless = webPath.isFlac() && webPath.getSession().isPrivileged();
        long length = getLength(track, lossless);

        Optional<BytesRange> range = BytesRange.read(webPath.header(RANGE), length).findFirst();
        if (range.filter(AbstractStreamer::unsatisfiable).isPresent()) {
            return Netty.respond(ctx, REQUESTED_RANGE_NOT_SATISFIABLE);
        }

        boolean keepAlive = webPath.isKeepAlive();

        HttpResponse response = prepareResponse(lossless, range.orElse(null), keepAlive);

        if (method == HttpMethod.GET) {
            Chunk chunk = range
                .map(bytesRange -> chunk(bytesRange, length))
                .orElseGet(() -> allOf(length));
            response.headers()
                .set(CONTENT_RANGE, chunk.range())
                .set(CONTENT_LENGTH, chunk.getSize());

            Object content = prepareContent(track, lossless, chunk);
            try {
                ctx.write(response);
                ChannelFuture streamingFuture = ctx.write(content, ctx.newProgressivePromise());
                if (!keepAlive) {
                    ctx.write(EMPTY_LAST_CONTENT).addListener(CLOSE);
                }
                listen(webPath, track, streamingFuture);
                return response;
            } finally {
                ctx.flush();
                webPath.getSession().streaming(chunk.getSize());
            }
        }

        response.headers().set(CONTENT_LENGTH, length);
        ctx.writeAndFlush(response);
        return response;
    }

    public HttpResponse prepareResponse(boolean lossless, BytesRange range, boolean keepAlive) {

        HttpResponseStatus status = range == null ? OK : PARTIAL_CONTENT;
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, status);
        response.headers()
            .set(CONTENT_TYPE, lossless ? WebPath.AUDIO_FLAC : WebPath.AUDIO_AAC)
            .set(ACCEPT_RANGES, BYTES);
        if (keepAlive) {
            response.headers().set(CONNECTION, KEEP_ALIVE);
        }
        return response;
    }

    public void listen(WebPath webPath, Track track, ChannelFuture streamingFuture) {

        if (streamingFuture instanceof ChannelProgressiveFuture) {
            ((ChannelProgressiveFuture) streamingFuture).addListener(
                new ProgressListener(String.format("%s %s", webPath.getSession(), track)));
        }
    }

    public Object prepareContent(Track track, boolean lossless, Chunk chunk) {

        Object content;
        try {
            content = content(track, chunk, lossless);
        } catch (Exception e) {
            throw new IllegalStateException(
                "Failed to prepare chunk " + chunk + " of " + (lossless ? "lossless " : "") + track, e);
        }
        return content;
    }

    public static boolean unsatisfiable(BytesRange r) {

        return !r.isSatisfiable();
    }

    protected Chunk chunk(BytesRange rangeHeader, long fileLength) {

        try {
            long start = rangeHeader.getStart();
            long rangeExclusiveEnd = rangeHeader.getExclusiveEnd(fileLength);
            long exclusiveEnd = min(
                rangeExclusiveEnd,
                start + fileLength,
                bytesPerChunk > 0
                    ? start + bytesPerChunk
                    : Long.MAX_VALUE);

            return new Chunk(start, exclusiveEnd, fileLength);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid range header", e);
        }
    }

    protected abstract Object content(Track track, Chunk chunk, boolean lossless);

    protected abstract long length(Track track, boolean lossless);

    protected Chunk allOf(long length) {

        return new Chunk(length);
    }

    private long getLength(Track track, boolean lossless) {

        return (lossless ? longLengths : shortLengths).computeIfAbsent(track, t -> length(track, lossless));
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
}
