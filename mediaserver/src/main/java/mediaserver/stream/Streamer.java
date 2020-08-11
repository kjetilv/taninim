package mediaserver.stream;

import java.time.Clock;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.LongStream;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import mediaserver.Config;
import mediaserver.http.Handling;
import mediaserver.http.NettyHandler;
import mediaserver.http.Req;
import mediaserver.http.Route;
import mediaserver.media.AlbumTrack;
import mediaserver.media.Media;
import mediaserver.media.Track;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.channel.ChannelFutureListener.CLOSE;
import static io.netty.handler.codec.http.HttpHeaderNames.ACCEPT_RANGES;
import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_RANGE;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.RANGE;
import static io.netty.handler.codec.http.HttpHeaderNames.USER_AGENT;
import static io.netty.handler.codec.http.HttpHeaderValues.BYTES;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.PARTIAL_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.REQUESTED_RANGE_NOT_SATISFIABLE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.handler.codec.http.LastHttpContent.EMPTY_LAST_CONTENT;

public abstract class Streamer extends NettyHandler {
    
    private static final Logger log = LoggerFactory.getLogger(Streamer.class);
    
    private final Clock clock;
    
    private final Supplier<Media> media;
    
    private final int bytesPerChunk;
    
    private final Map<Track, Long> shortLengths = new ConcurrentHashMap<>();
    
    private final Map<Track, Long> longLengths = new ConcurrentHashMap<>();
    
    Streamer(Route route, Clock clock, Supplier<Media> media, int bytesPerChunk) {
        super(route);
        this.clock = clock;
        this.media = media;
        this.bytesPerChunk = bytesPerChunk;
        log.info("{} created", this);
    }
    
    @Override
    
    protected Handling handle(Req req) {
        HttpMethod method = req.getRequest().method();
        if (method == HttpMethod.HEAD || method == HttpMethod.GET) {
            return media.get().getAlbumTrack(uuid(req.getPath(true)))
                .findFirst()
                .map(albumTrack ->
                    StreamAuthorization.authorizedStreaming(req, albumTrack, this.media)
                        ? handle(req, albumTrack)
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
    
    protected abstract Object content(Track track, Chunk chunk, boolean lossless);
    
    protected abstract long trackLength(Track track, boolean lossless);
    
    private Chunk chunk(Range rangeHeader, long fileLength, boolean truncate) {
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
    
    private Handling handle(Req req, AlbumTrack albumTrack) {
        boolean lossless = req.isFlac() && req.isLocal();
        if (req.getRequest().method() == HttpMethod.HEAD) {
            return handledMeta(req, albumTrack, lossless);
        }
        return Range.read(req.header(RANGE), length(albumTrack.getTrack(), lossless))
            .filter(Range::isSatisfiable)
            .map(range ->
                handledPartial(req, albumTrack, lossless, range))
            .findFirst()
            .orElseGet(() ->
                handle(req, REQUESTED_RANGE_NOT_SATISFIABLE));
    }
    
    private Handling handledMeta(Req req, AlbumTrack albumTrack, boolean lossless) {
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
        response.headers()
            .set(CONTENT_LENGTH, length(albumTrack.getTrack(), lossless))
            .set(CONTENT_TYPE, lossless ? AUDIO_FLAC : AUDIO_AAC)
            .set(ACCEPT_RANGES, BYTES);
        if (req.isKeepAlive()) {
            response.headers().set(CONNECTION, KEEP_ALIVE);
        }
        req.getCtx().writeAndFlush(response);
        return handled(req, response);
    }
    
    private Handling handledPartial(Req req, AlbumTrack albumTrack, boolean lossless, Range range) {
        HttpResponse response =
            new DefaultHttpResponse(HTTP_1_1, range == null ? OK : PARTIAL_CONTENT);
        long length = length(albumTrack.getTrack(), lossless);
        Chunk chunk = range == null
            ? new Chunk(length)
            : chunk(range, length, !isVlc(req));
        response.headers()
            .set(CONTENT_TYPE, lossless ? AUDIO_FLAC : AUDIO_AAC)
            .set(ACCEPT_RANGES, BYTES)
            .set(CONTENT_RANGE, chunk.getRangeHeader())
            .set(CONTENT_LENGTH, chunk.getSize());
        if (req.isKeepAlive()) {
            response.headers().set(CONNECTION, KEEP_ALIVE);
        }
        Object content;
        try {
            content = content(albumTrack.getTrack(), chunk, lossless);
        } catch (Exception e) {
            throw new IllegalStateException(
                "Failed to prepare chunk " + chunk + " of " + (lossless ? "lossless " : "") + albumTrack, e);
        }
        try {
            ChannelHandlerContext ctx = req.getCtx();
            ctx.write(response);
            ctx.write(content, ctx.newProgressivePromise()
                .addListener(new ProgressListener(clock, req, albumTrack, range, chunk)));
            ChannelFuture lastContentFuture = ctx.writeAndFlush(EMPTY_LAST_CONTENT);
            if (!req.isKeepAlive()) {
                lastContentFuture.addListener(CLOSE);
            }
            return handled(req, response);
        } finally {
            req.getSession().addBytesStreamed(chunk.getSize());
        }
    }
    
    private long length(Track track, boolean lossless) {
        Map<Track, Long> map = lossless ? longLengths : shortLengths;
        return map.computeIfAbsent(track, t -> trackLength(track, lossless));
    }
    
    private static final String AUDIO_FLAC = "audio/flac";
    
    private static final String AUDIO_AAC = "audio/m4a";
    
    private static boolean isVlc(Req req) {
        return req.header(USER_AGENT).startsWith("VLC");
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
