package mediaserver;

import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.SystemPropertyUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

class HttpStatic extends SimpleChannelInboundHandler<HttpRequest> {

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpRequest req) throws Exception {
        if (!req.decoderResult().isSuccess()) {
            sendError(ctx, BAD_REQUEST);
            return;
        }

        String path = normalize(req.uri());
        if (path == null) {
            sendError(ctx, FORBIDDEN);
            return;
        }

        File file = new File(path);
        if (file.isHidden() || !file.exists()) {
            sendError(ctx, NOT_FOUND);
            return;
        }

        if (!file.isFile()) {
            sendError(ctx, FORBIDDEN);
            return;
        }

        RandomAccessFile raf;
        try {
            raf = new RandomAccessFile(file, "r");
        } catch (FileNotFoundException ignore) {
            sendError(ctx, NOT_FOUND);
            return;
        }
        long fileLength = raf.length();

        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
        response.headers().set(CONTENT_TYPE, "audio/flac");
        if (HttpUtil.isKeepAlive(req)) {
            response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        // Write the content.
        ChannelFuture sendFileFuture;
        ChannelFuture lastContentFuture;

        // Tell clients that Partial Requests are available.
        response.headers().add(HttpHeaderNames.ACCEPT_RANGES, HttpHeaderValues.BYTES);

        String rangeHeader = req.headers().get(HttpHeaderNames.RANGE);
        System.out.println(HttpHeaderNames.RANGE + " = " + rangeHeader);
        if (rangeHeader != null && rangeHeader.length() > 0) { // Partial Request
            PartialRequestInfo partialRequestInfo = getPartialRequestInfo(rangeHeader, fileLength);

            // Set Response Header
            response.headers().add(HttpHeaderNames.CONTENT_RANGE, HttpHeaderValues.BYTES + " "
                + partialRequestInfo.startOffset + "-" + partialRequestInfo.endOffset + "/" + fileLength);
            System.out.println(
                HttpHeaderNames.CONTENT_RANGE + " : " + response.headers().get(HttpHeaderNames.CONTENT_RANGE));

            HttpUtil.setContentLength(response, partialRequestInfo.getChunkSize());
            System.out.println(HttpHeaderNames.CONTENT_LENGTH + " : " + partialRequestInfo.getChunkSize());

            response.setStatus(HttpResponseStatus.PARTIAL_CONTENT);

            // Write Response
            ctx.write(response);
            sendFileFuture = ctx.write(new DefaultFileRegion(raf.getChannel(), partialRequestInfo.getStartOffset(),
                partialRequestInfo.getChunkSize()), ctx.newProgressivePromise());
        } else {
            // Set Response Header
            HttpUtil.setContentLength(response, fileLength);

            // Write Response
            ctx.write(response);
            sendFileFuture = ctx.write(new DefaultFileRegion(raf.getChannel(), 0, fileLength),
                ctx.newProgressivePromise());
        }

        lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

        sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
            @Override
            public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
                if (total < 0) { // total unknown
                    System.err.println(future.channel() + " Transfer progress: " + progress);
                } else {
                    System.err.println(future.channel() + " Transfer progress: " + progress + " / " + total);
                }
            }

            @Override
            public void operationComplete(ChannelProgressiveFuture future) {
                System.err.println(future.channel() + " Transfer complete.");
            }
        });

        // Decide whether to close the connection or not.
        if (!HttpUtil.isKeepAlive(req)) {
            // Close the connection when the whole content is written out.
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        if (ctx.channel().isActive()) {
            sendError(ctx, INTERNAL_SERVER_ERROR);
        }
    }

    private static String normalize(String inputUri) {
        String uri = URLDecoder.decode(inputUri, StandardCharsets.UTF_8);
        if (uri.isEmpty() || uri.charAt(0) != '/') {
            return null;
        }
        return SystemPropertyUtil.get("user.dir") + File.separator + uri;
    }

    private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status,
            Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private PartialRequestInfo getPartialRequestInfo(String rangeHeader, long fileLength) {
        long startOffset;
        try {
            startOffset = Integer.parseInt(rangeHeader.trim()
                .replace(
                    HttpHeaderValues.BYTES + "=",
                    "")
                .replace("-",
                    ""));
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Unvalid range header", e);
        }
        long endOffset = endOffset(fileLength, startOffset);
        long chunkSize = endOffset - startOffset + 1;

        PartialRequestInfo partialRequestInfo = new PartialRequestInfo();
        partialRequestInfo.setStartOffset(startOffset);
        partialRequestInfo.setEndOffset(endOffset);
        partialRequestInfo.setChunkSize(chunkSize);
        return partialRequestInfo;
    }

    private long endOffset(long fileLength, long startOffset) {
        long endOffset = startOffset + fileLength;
        return endOffset < fileLength ? endOffset : fileLength - 1;
    }

    @SuppressWarnings("unused")
    static class PartialRequestInfo {

        private long startOffset;

        private long endOffset;

        private long chunkSize;

        public long getEndOffset() {
            return endOffset;
        }

        void setEndOffset(long endOffset) {
            this.endOffset = endOffset;
        }

        long getStartOffset() {
            return startOffset;
        }

        void setStartOffset(long startOffset) {
            this.startOffset = startOffset;
        }

        long getChunkSize() {
            return chunkSize;
        }

        void setChunkSize(long chunkSize) {
            this.chunkSize = chunkSize;
        }
    }
}
