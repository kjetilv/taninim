package mediaserver.gui;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import mediaserver.Config;
import mediaserver.http.*;
import mediaserver.media.Media;
import mediaserver.media.Track;
import mediaserver.sessions.AccessLevel;
import mediaserver.toolkit.Chunk;
import mediaserver.toolkit.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
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

    private static final Logger log = LoggerFactory.getLogger(AbstractStreamer.class);

    private final Clock clock;

    protected final Supplier<Media> media;

    private final int bytesPerChunk;

    private final Map<Track, Long> shortLengths = new ConcurrentHashMap<>();

    private final Map<Track, Long> longLengths = new ConcurrentHashMap<>();

    AbstractStreamer(Clock clock, Supplier<Media> media, int bytesPerChunk) {

        super(Page.AUDIO);

        this.clock = clock;
        this.media = media;
        this.bytesPerChunk = bytesPerChunk;

        log.info("{} created", this);
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

        return getClass().getSimpleName() + "[" + media + ", kb/chunk:" + bytesPerChunk / Config.K + "]";
    }

    public boolean authorized(WebPath webPath, Track track) {

        return Streamer.isAuthorized(webPath, track, this.media);
    }

    @Override
    public HttpResponse stream(WebPath webPath, Track track) {

        HttpMethod method = webPath.getRequest().method();
        return method == HttpMethod.HEAD || method == HttpMethod.GET
            ? stream(webPath, track, method)
            : Netty.respond(webPath.getCtx(), BAD_REQUEST);

    }

    public Object prepareContent(Track track, boolean lossless, Chunk chunk) {

        try {
            return content(track, chunk, lossless);
        } catch (Exception e) {
            throw new IllegalStateException(
                "Failed to prepare chunk " + chunk + " of " + (lossless ? "lossless " : "") + track, e);
        }
    }

    public static boolean unsatisfiable(Range r) {

        return !r.isSatisfiable();
    }

    protected Chunk chunk(Range rangeHeader, long fileLength) {

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

    protected abstract long trackLength(Track track, boolean lossless);

    protected Chunk allOf(long length) {

        return new Chunk(length);
    }

    private HttpResponse stream(WebPath webPath, Track track, HttpMethod method) {

        boolean lossless = webPath.isFlac() && webPath.getSession().isPrivileged();
        if (method == HttpMethod.HEAD) {
            return respondMeta(webPath, track, lossless);
        }

        Optional<Range> range = Range.read(webPath.header(RANGE), length(track, lossless)).findFirst();
        if (range.filter(AbstractStreamer::unsatisfiable).isPresent()) {
            return respondUnsatisfiable(webPath);
        }

        return respondPartial(webPath, track, lossless, range.orElse(null));
    }

    private HttpResponse respondMeta(WebPath webPath, Track track, boolean lossless) {

        HttpResponse response =
            new DefaultHttpResponse(HTTP_1_1, OK);
        response.headers()
            .set(CONTENT_LENGTH, length(track, lossless))
            .set(CONTENT_TYPE, lossless ? WebPath.AUDIO_FLAC : WebPath.AUDIO_AAC)
            .set(ACCEPT_RANGES, BYTES);
        if (webPath.isKeepAlive()) {
            response.headers().set(CONNECTION, KEEP_ALIVE);
        }
        webPath.getCtx().writeAndFlush(response);
        return response;
    }

    private HttpResponse respondUnsatisfiable(WebPath webPath) {

        return Netty.respond(webPath.getCtx(), REQUESTED_RANGE_NOT_SATISFIABLE);
    }

    private HttpResponse respondPartial(WebPath webPath, Track track, boolean lossless, Range range) {

        HttpResponse response =
            new DefaultHttpResponse(HTTP_1_1, range == null ? OK : PARTIAL_CONTENT);
        response.headers()
            .set(CONTENT_TYPE, lossless ? WebPath.AUDIO_FLAC : WebPath.AUDIO_AAC)
            .set(ACCEPT_RANGES, BYTES);

        if (webPath.isKeepAlive()) {
            response.headers().set(CONNECTION, KEEP_ALIVE);
        }

        Chunk chunk = range == null
            ? allOf(length(track, lossless))
            : chunk(range, length(track, lossless));

        response.headers()
            .set(CONTENT_RANGE, chunk.getRangeHeader())
            .set(CONTENT_LENGTH, chunk.getSize());

        Object content = prepareContent(track, lossless, chunk);

        ChannelHandlerContext ctx = webPath.getCtx();
        try {
            ctx.write(response);
            ctx.write(content, ctx.newProgressivePromise()
                .addListener(new ProgressListener(clock, webPath, track, range, chunk)));
            ChannelFuture last = ctx.writeAndFlush(EMPTY_LAST_CONTENT);
            if (!webPath.isKeepAlive()) {
                last.addListener(CLOSE);
            }
        } finally {
            webPath.getSession().streaming(chunk.getSize());
        }

        return response;
    }

    private long length(Track track, boolean lossless) {

        return (lossless ? longLengths : shortLengths).computeIfAbsent(track, t -> trackLength(track, lossless));
    }

    private Optional<Track> getMediaTrack(String path) {

        return media.get().getTrack(uuid(path));
    }

    private static UUID uuid(String path) {

        try {
            int typeIndex = path.indexOf('.', 1);
            String uuidPart = typeIndex < 0 ? path : path.substring(0, typeIndex);
            return UUID.fromString(uuidPart);
        } catch (Exception e) {
            throw new IllegalArgumentException("Ugyldig UUID: " + path, e);
        }
    }

    private static long min(long... lengths) {

        return LongStream.of(lengths).min().orElseThrow(() ->
            new IllegalStateException("No end in sight! " + Arrays.toString(lengths)));
    }
}
