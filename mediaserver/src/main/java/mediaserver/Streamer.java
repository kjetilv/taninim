package mediaserver;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.*;
import mediaserver.files.Media;
import mediaserver.files.Track;
import mediaserver.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Optional;
import java.util.UUID;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

final class Streamer extends Nettish {

    private static final Logger log = LoggerFactory.getLogger(Streamer.class);

    private final Media media;
    private static final String AUDIO_FLAC = "audio/flac";

    public Streamer(IO io, Media media) {
        super(io, "/audio");
        this.media = media;
    }

    @Override
    public void handle(HttpRequest req, String path, ChannelHandlerContext ctx) {
        getFile(path)
            .filter(File::isFile)
            .filter(File::canRead)
            .map(file ->
                streamFile(req, file, ctx));
    }

    private static boolean streamFile(HttpRequest req, File file, ChannelHandlerContext ctx) {
        RandomAccessFile randomAccessFile = randomAccess(file);
        long fileLength = length(randomAccessFile);

        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
        response.headers().set(CONTENT_TYPE, AUDIO_FLAC);
        if (HttpUtil.isKeepAlive(req)) {
            response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        response.headers().add(HttpHeaderNames.ACCEPT_RANGES, HttpHeaderValues.BYTES);

        String rangeHeader = req.headers().get(HttpHeaderNames.RANGE);

        ChannelFuture sendFile = rangeHeader != null && rangeHeader.length() > 0
            ? writePartial(ctx, randomAccessFile, fileLength, response, rangeHeader)
            : write(ctx, randomAccessFile, fileLength, response);
        ChannelFuture lastContentFuture =
            ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

        sendFile.addListener(new ProgressListener(file));

        if (!HttpUtil.isKeepAlive(req)) {
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }
        return true;
    }

    private static RandomAccessFile randomAccess(File file) {
        try {
            return new RandomAccessFile(file, "r");
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Expected to find file " + file, e);
        }
    }

    private Optional<File> getFile(String path) {
        UUID uuid = UUID.fromString(path.substring(1));
        return media.getSong(uuid).map(Track::getFile);
    }

    private static ChannelFuture write(
        ChannelHandlerContext ctx,
        RandomAccessFile file,
        long len,
        HttpResponse response
    ) {
        HttpUtil.setContentLength(response, len);
        ctx.write(response);
        return ctx.write(
            new DefaultFileRegion(file.getChannel(), 0, len),
            ctx.newProgressivePromise());
    }

    private static ChannelFuture writePartial(
        ChannelHandlerContext ctx,
        RandomAccessFile file,
        long len,
        HttpResponse response,
        String rangeHeader
    ) {
        PartialRequestInfo pri = getPartialRequestInfo(rangeHeader, len);
        response.headers().add(HttpHeaderNames.CONTENT_RANGE,
            HttpHeaderValues.BYTES + " " + pri.getStartOffset() + "-" + pri.getEndOffset() + "/" + len);
        log.info("{}: {}", HttpHeaderNames.CONTENT_RANGE, response.headers().get(HttpHeaderNames.CONTENT_RANGE));

        HttpUtil.setContentLength(response, pri.getChunkSize());
        log.info("{}: {}", HttpHeaderNames.CONTENT_LENGTH, pri.getChunkSize());

        response.setStatus(HttpResponseStatus.PARTIAL_CONTENT);

        ctx.write(response);
        return ctx.write(new DefaultFileRegion(file.getChannel(), pri.getStartOffset(),
            pri.getChunkSize()), ctx.newProgressivePromise());
    }

    private static long length(RandomAccessFile randomAccessFile) {
        try {
            return randomAccessFile.length();
        } catch (IOException e) {
            throw new IllegalStateException("Could not read length of " + randomAccessFile, e);
        }
    }

    private static PartialRequestInfo getPartialRequestInfo(String rangeHeader, long fileLength) {
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

    private static long endOffset(long fileLength, long startOffset) {
        long endOffset = startOffset + fileLength;
        return endOffset < fileLength ? endOffset : fileLength - 1;
    }

}
