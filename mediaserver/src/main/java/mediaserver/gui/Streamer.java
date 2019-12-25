package mediaserver.gui;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import mediaserver.externals.FacebookUser;
import mediaserver.http.Handling;
import mediaserver.http.NettyHandler;
import mediaserver.http.Prefix;
import mediaserver.http.WebPath;
import mediaserver.media.Media;
import mediaserver.media.Track;
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

public abstract class Streamer extends NettyHandler {

    protected final Supplier<Media> media;

    private final Sessions sessions;

    private static final int BYTES_PREAMBLE = (BYTES + "=").length();

    Streamer(Supplier<Media> media, Sessions sessions) {

        super(Prefix.AUDIO);
        this.media = media;
        this.sessions = sessions;
    }

    @Override
    public Handling handleRequest(WebPath webPath, ChannelHandlerContext ctx) {

        return sessions.activeUser(webPath)
            .map(user ->
                getMediaTrack(resource(webPath))
                    .map(trackStreamer(webPath, user, ctx))
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
        FacebookUser user,
        Track track,
        ChannelHandlerContext ctx,
        HttpResponse res
    );

    protected static boolean isFlac(HttpRequest req) {

        return req.uri().endsWith("." + "flac");
    }

    protected ChannelFuture writeLength(
        ChannelHandlerContext ctx,
        HttpResponse response,
        long fileLength
    ) {

        HttpUtil.setContentLength(response, fileLength);
        return ctx.write(response);
    }

    protected static ProgressListener progressListener(FacebookUser user, Track track) {

        return new ProgressListener(String.format("%s %s", user, track));
    }

    private Function<Track, Handling> trackStreamer(
        WebPath webPath,
        FacebookUser user,
        ChannelHandlerContext ctx
    ) {

        return track -> {
            HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
            response.headers().set(CONTENT_TYPE, webPath.getContentType());
            boolean keepAlive = webPath.isKeepAlive();
            if (keepAlive) {
                response.headers().set(CONNECTION, KEEP_ALIVE);
            }
            response.headers().add(ACCEPT_RANGES, BYTES);
            stream(webPath, user, track, ctx, response)
                .ifPresent(channelFuture ->
                    channelFuture.addListener(progressListener(user, track)));
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

        int dotIndex = path.indexOf('.', 1);
        String uuidString = path.substring(1, dotIndex);
        return media.get().getTrack(UUID.fromString(uuidString));
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + sessions + "]";
    }
}
