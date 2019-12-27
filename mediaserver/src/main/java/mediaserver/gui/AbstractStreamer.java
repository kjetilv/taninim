package mediaserver.gui;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpUtil;
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
import java.util.function.Function;
import java.util.function.Supplier;

import static io.netty.channel.ChannelFutureListener.CLOSE;
import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.BYTES;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.handler.codec.http.LastHttpContent.EMPTY_LAST_CONTENT;

public abstract class AbstractStreamer extends NettyHandler {

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
                    .map(trackStreamer(webPath, session, ctx))
                    .orElseGet(() ->
                        handle(ctx, NOT_FOUND)))
            .orElseGet(() ->
                handle(ctx, UNAUTHORIZED));
    }

    static void updateHeaders(HttpResponse response, PartialRequestInfo pri, long length) {

        response.headers().add(
            CONTENT_RANGE,
            BYTES + " " + pri.getStartOffset() + "-" + pri.getEndOffset() + "/" + length);
        HttpUtil.setContentLength(response, pri.getChunkSize());
        response.setStatus(PARTIAL_CONTENT);
    }

    protected static PartialRequestInfo getPartialRequestInfo(String rangeHeader, long length) {

        try {
            long startOffset = Integer.parseInt(rangeHeader.substring(BYTES_PREAMBLE, rangeHeader.indexOf("-")));
            long endOffset = endOffset(length, startOffset);
            long chunkSize = endOffset - startOffset + 1;
            return new PartialRequestInfo(startOffset, endOffset, chunkSize);
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

    protected static ProgressListener progressListener(Session session, Track track) {

        return new ProgressListener(String.format("%s %s", session, track));
    }

    private Function<Track, Handling> trackStreamer(
        WebPath webPath,
        Session session,
        ChannelHandlerContext ctx
    ) {

        return track -> {
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
            ChannelFuture lastContentFuture =
                ctx.writeAndFlush(EMPTY_LAST_CONTENT);
            if (!keepAlive) {
                lastContentFuture.addListener(CLOSE);
            }
            return handle(ctx, response);
        };
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
