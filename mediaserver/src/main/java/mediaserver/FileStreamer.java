package mediaserver;

import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

final class FileStreamer {

    private static final Logger log = LoggerFactory.getLogger(FileStreamer.class);

    public static <T> T sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status,
            Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        return null;
    }

    static File stream(HttpRequest req, String path, ChannelHandlerContext ctx) {
        File file = new File(path);
        if (file.isHidden() || !file.exists()) {
            return sendError(ctx, NOT_FOUND);
        }
        if (!file.isFile()) {
            return sendError(ctx, FORBIDDEN);
        }
        RandomAccessFile randomAccessFile;
        try {
            randomAccessFile = new RandomAccessFile(file, "r");
        } catch (FileNotFoundException ignore) {
            return sendError(ctx, NOT_FOUND);
        }
        long fileLength = length(randomAccessFile);

        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
        response.headers().set(CONTENT_TYPE, "audio/flac");
        if (HttpUtil.isKeepAlive(req)) {
            response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        response.headers().add(HttpHeaderNames.ACCEPT_RANGES, HttpHeaderValues.BYTES);

        String rangeHeader = req.headers().get(HttpHeaderNames.RANGE);
        System.out.println(HttpHeaderNames.RANGE + " = " + rangeHeader);

        ChannelFuture sendFile = rangeHeader != null && rangeHeader.length() > 0
            ? writePartial(ctx, randomAccessFile, fileLength, response, rangeHeader)
            : write(ctx, randomAccessFile, fileLength, response);
        ChannelFuture lastContentFuture =
            ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

        sendFile.addListener(new ChannelProgressiveFutureListener() {
            @Override
            public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
                if (total < 0) { // total unknown
                    log.info("{} Transfer progress: {} {}", future.channel(), file, progress);
                } else {
                    log.info("{} Transfer progress: {} {}/{}", future.channel(), file, progress, total);
                }
            }

            @Override
            public void operationComplete(ChannelProgressiveFuture future) {
                log.info("{} Transfer complete: {}", future.channel(), file);
            }
        });

        // Decide whether to close the connection or not.
        if (!HttpUtil.isKeepAlive(req)) {
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }
        return file;
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
