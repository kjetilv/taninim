package mediaserver.gui;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import mediaserver.Media;
import mediaserver.files.Track;
import mediaserver.sessions.Sessions;
import mediaserver.util.IO;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public abstract class AbstractStreamer extends Nettish {

    protected static final String FLAC = "flac";

    protected static final String AAC = "aac";

    protected static final String M4A = "m4a";

    protected static final String AUDIO_FLAC = "audio/" + FLAC;

    protected static final String AUDIO_AAC = "audio/" + AAC;

    protected final Supplier<Media> media;

    private final Sessions sessions;

    private static final int BYTES_PREAMBLE = (HttpHeaderValues.BYTES + "=").length();

    AbstractStreamer(IO io, Supplier<Media> media, Sessions sessions) {

        super(io, "/audio");
        this.media = media;
        this.sessions = sessions;
    }

    @Override
    public HttpResponse handle(FullHttpRequest req, String path, ChannelHandlerContext ctx) {

        return activeUserByCookie(req)
            .map(user ->
                getMediaTrack(resource(path))
                    .map(track -> {
                        HttpResponse response = response(req);
                        stream(req, track, ctx, response);
                        return respondStream(req, ctx, response);
                    })
                    .orElseGet(() ->
                        respond(ctx, path, NOT_FOUND)))
            .orElseGet(() ->
                teapot(req, ctx));
    }

    public Optional<String> activeUserByCookie(FullHttpRequest req) {

        return authCookie(req).flatMap(sessions::activeUser);
    }

    static void updateHeaders(HttpResponse response, PartialRequestInfo pri, long length) {

        response.headers().add(
            HttpHeaderNames.CONTENT_RANGE,
            HttpHeaderValues.BYTES + " " + pri.getStartOffset() + "-" + pri.getEndOffset() + "/" + length);
        HttpUtil.setContentLength(response, pri.getChunkSize());
        response.setStatus(HttpResponseStatus.PARTIAL_CONTENT);
    }

    protected static HttpResponse response(HttpRequest req) {

        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
        response.headers().set(CONTENT_TYPE, contentType(req));
        if (HttpUtil.isKeepAlive(req)) {
            response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        response.headers().add(HttpHeaderNames.ACCEPT_RANGES, HttpHeaderValues.BYTES);
        return response;
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

    protected abstract ChannelFuture stream(
        HttpRequest req,
        Track track,
        ChannelHandlerContext ctx,
        HttpResponse res
    );

    protected static String audioType(HttpRequest req) {

        return isFlac(req) ? FLAC : M4A;
    }

    protected static boolean isFlac(HttpRequest req) {

        return req.uri().endsWith("." + FLAC);
    }

    protected HttpResponse teapot(HttpRequest req, ChannelHandlerContext ctx) {

        return respond(ctx, req.uri(), HttpResponseStatus.valueOf(418, "I'm a teapot"));
    }

    protected HttpResponse respondStream(HttpRequest req, ChannelHandlerContext ctx, HttpResponse response) {

        ChannelFuture lastContentFuture =
            ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        if (!HttpUtil.isKeepAlive(req)) {
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }
        return response;
    }

    protected ChannelFuture writeLength(ChannelHandlerContext ctx, HttpResponse response, long fileLength) {

        HttpUtil.setContentLength(response, fileLength);
        return ctx.write(response);
    }

    private static String contentType(HttpRequest req) {

        return req.uri().endsWith(FLAC) ? AUDIO_FLAC : AUDIO_AAC;
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
