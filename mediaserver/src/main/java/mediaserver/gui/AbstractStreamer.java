package mediaserver.gui;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.util.concurrent.GenericFutureListener;
import mediaserver.http.Handling;
import mediaserver.http.NettyHandler;
import mediaserver.http.Prefix;
import mediaserver.http.WebPath;
import mediaserver.media.Media;
import mediaserver.media.Track;
import mediaserver.sessions.Session;
import mediaserver.sessions.Sessions;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static io.netty.channel.ChannelFutureListener.CLOSE;
import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.BYTES;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.handler.codec.http.LastHttpContent.EMPTY_LAST_CONTENT;

public abstract class AbstractStreamer extends NettyHandler implements Streamer {

    protected final Supplier<Media> media;

    private final Sessions sessions;

    private static final int BYTES_PREAMBLE = (BYTES + "=").length();

    AbstractStreamer(Supplier<Media> media, Sessions sessions) {

        super(Prefix.AUDIO);
        this.media = media;
        this.sessions = sessions;
    }

    @Override
    public Handling handleRequest(WebPath webPath, ChannelHandlerContext ctx) {

        return sessions.activeSession(webPath)
            .map(session ->
                getMediaTrack(webPath.getPath(true))
                    .map(track ->
                        stream(webPath, session, track, ctx))
                    .orElseGet(() ->
                        handle(ctx, NOT_FOUND)))
            .orElseGet(() ->
                handle(ctx, UNAUTHORIZED));
    }

    @Override
    public Handling stream(WebPath webPath, Session session, Track track, ChannelHandlerContext ctx) {

        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
        boolean lossless = webPath.isFlac() && session.isPrivileged();
        response.headers().set(CONTENT_TYPE, lossless
            ? WebPath.AUDIO_FLAC
            : WebPath.AUDIO_AAC);
        boolean keepAlive = webPath.isKeepAlive();
        if (keepAlive) {
            response.headers().set(CONNECTION, KEEP_ALIVE);
        }
        response.headers().add(ACCEPT_RANGES, BYTES);

        stream(webPath, session, track, lossless, response, ctx)
            .ifPresent(channelFuture ->
                channelFuture.addListener(progressListener(session, track)));

        ChannelFuture lastContentFuture = ctx.writeAndFlush(EMPTY_LAST_CONTENT);

        if (!keepAlive) {
            lastContentFuture.addListener(CLOSE);
        }
        return handled(response);
    }

    protected static Chunk chunk(String rangeHeader, long fileLength) {

        try {
            long startOffset = Integer.parseInt(rangeHeader.substring(BYTES_PREAMBLE, rangeHeader.indexOf("-")));
            long endOffset = endOffset(fileLength, startOffset);
            long chunkSize = endOffset - startOffset + 1;
            return new Chunk(startOffset, endOffset, chunkSize, fileLength);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid range header", e);
        }
    }

    protected abstract Optional<ChannelFuture> stream(
        WebPath webPath,
        Session user,
        Track track,
        boolean lossless,
        HttpResponse response,
        ChannelHandlerContext ctx
    );

    protected ChannelFuture writeLength(
        ChannelHandlerContext ctx,
        HttpResponse response,
        long fileLength
    ) {

        HttpUtil.setContentLength(response, fileLength);
        return ctx.write(response);
    }

    protected static GenericFutureListener<ChannelProgressiveFuture> progressListener(
        Session session, Track track
    ) {

        return new ProgressListener(String.format("%s %s", session, track));
    }

    protected ChannelFuture writeContent(
        Session session, ChannelHandlerContext ctx,
        Chunk chunk,
        HttpResponse response,
        Object content
    ) {

        response.headers().add(
            CONTENT_RANGE,
            BYTES + " " + chunk.getStartOffset() + "-" + chunk.getEndOffset() + "/" + chunk.getTotalSize());
        HttpUtil.setContentLength(response, chunk.getSize());
        response.setStatus(PARTIAL_CONTENT);
        try {
            ctx.write(response);
            return ctx.writeAndFlush(content, ctx.newProgressivePromise());
        } finally {
            session.streaming(chunk.getSize());
        }
    }

    private static long endOffset(long fileLength, long startOffset) {

        long endOffset = startOffset + fileLength;
        return endOffset < fileLength ? endOffset : fileLength - 1;
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

        return getClass().getSimpleName() + "[" + sessions + "]";
    }
}
