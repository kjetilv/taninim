package mediaserver.stream;

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
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.LongStream;

import static io.netty.channel.ChannelFutureListener.CLOSE;
import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.BYTES;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.handler.codec.http.LastHttpContent.EMPTY_LAST_CONTENT;

public abstract class Streamer extends NettyHandler {

    private static final Logger log = LoggerFactory.getLogger(Streamer.class);

    private final Clock clock;

    private final Supplier<Media> media;

    private final int bytesPerChunk;

    private final Map<Track, Long> shortLengths = new ConcurrentHashMap<>();

    private final Map<Track, Long> longLengths = new ConcurrentHashMap<>();

    Streamer(Clock clock, Supplier<Media> media, int bytesPerChunk) {

        super(Page.AUDIO);

        this.clock = clock;
        this.media = media;
        this.bytesPerChunk = bytesPerChunk;

        log.info("{} created", this);
    }

    public static Predicate<Track> authorized(Media media, Req req) {

        return track -> authorized(media, req, track);
    }

    public static boolean authorized(Media media, Req req, Track track) {

        return authorized(req, track, () -> media);
    }

    @Override
    protected Handling handleRequest(Req req) {

        HttpMethod method = req.getRequest().method();
        if (method == HttpMethod.HEAD || method == HttpMethod.GET) {
            return getMediaTrack(req.getPath(true))
                .map(track -> authorized(req, track, this.media)
                    ? handled(req, respond(req, track))
                    : handleUnauthorized(req))
                .orElseGet(() ->
                    handleNotFound(req));
        }
        return handleBadRequest(req);
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + media + ", kb/chunk:" + bytesPerChunk / Config.K + "]";
    }

    protected Chunk chunk(Range rangeHeader, long fileLength, boolean truncate) {

        try {
            long start = rangeHeader.getStart();
            long rangeExclusiveEnd = rangeHeader.getExclusiveEnd(fileLength);
            long exclusiveEnd = min(
                rangeExclusiveEnd,
                start + fileLength,
                truncate && bytesPerChunk > 0
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

    private static boolean authorized(Req req, Track track, Supplier<Media> media) {

        AccessLevel accessLevel = req.getAccessLevel();
        return accessLevel.satisfies(AccessLevel.STREAM) ||
            accessLevel.satisfies(AccessLevel.STREAM_CURATED) && media.get().isCurated(track);
    }

    private HttpResponse respond(Req req, Track track) {

        boolean lossless = req.isFlac() && req.getSession().isPrivileged();
        if (req.getRequest().method() == HttpMethod.HEAD) {
            return respondMeta(req, track, lossless);
        }

        Optional<Range> range = Range.read(req.header(RANGE), length(track, lossless)).findFirst();
        if (range.filter(Streamer::unsatisfiable).isPresent()) {
            return respondUnsatisfiable(req);
        }

        return respondPartial(req, track, lossless, range.orElse(null));
    }

    private static boolean unsatisfiable(Range r) {

        return !r.isSatisfiable();
    }

    private HttpResponse respondMeta(Req req, Track track, boolean lossless) {

        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
        response.headers()
            .set(CONTENT_LENGTH, length(track, lossless))
            .set(CONTENT_TYPE, lossless ? Req.AUDIO_FLAC : Req.AUDIO_AAC)
            .set(ACCEPT_RANGES, BYTES);
        if (req.isKeepAlive()) {
            response.headers().set(CONNECTION, KEEP_ALIVE);
        }
        req.getCtx().writeAndFlush(response);
        return response;
    }

    private HttpResponse respondUnsatisfiable(Req req) {

        return Netty.respond(req.getCtx(), REQUESTED_RANGE_NOT_SATISFIABLE);
    }

    private HttpResponse respondPartial(Req req, Track track, boolean lossless, Range range) {

        HttpResponse response =
            new DefaultHttpResponse(HTTP_1_1, range == null ? OK : PARTIAL_CONTENT);

        Chunk chunk = range == null
            ? allOf(length(track, lossless))
            : chunk(range, length(track, lossless), !isVlc(req));

        response.headers()
            .set(CONTENT_TYPE, lossless ? Req.AUDIO_FLAC : Req.AUDIO_AAC)
            .set(ACCEPT_RANGES, BYTES)
            .set(CONTENT_RANGE, chunk.getRangeHeader())
            .set(CONTENT_LENGTH, chunk.getSize());
        if (req.isKeepAlive()) {
            response.headers().set(CONNECTION, KEEP_ALIVE);
        }

        Object content;
        try {
            content = content(track, chunk, lossless);
        } catch (Exception e) {
            throw new IllegalStateException(
                "Failed to prepare chunk " + chunk + " of " + (lossless ? "lossless " : "") + track, e);
        }

        try {
            ChannelHandlerContext ctx = req.getCtx();
            ctx.write(response);
            ctx.write(content, ctx.newProgressivePromise()
                .addListener(new ProgressListener(clock, req, track, range, chunk)));
            ChannelFuture lastContentFuture = ctx.writeAndFlush(EMPTY_LAST_CONTENT);
            if (!req.isKeepAlive()) {
                lastContentFuture.addListener(CLOSE);
            }
            return response;
        } finally {
            req.getSession().streaming(chunk.getSize());
        }
    }

    private static boolean isVlc(Req req) {

        return req.header(USER_AGENT).startsWith("VLC");
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
