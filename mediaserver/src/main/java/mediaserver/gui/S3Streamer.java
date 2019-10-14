package mediaserver.gui;

import io.minio.MinioClient;
import io.minio.ObjectStat;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import mediaserver.files.Media;
import mediaserver.files.Track;
import mediaserver.util.IO;

import java.io.*;
import java.util.UUID;

public final class S3Streamer extends AbstractStreamer {

    private static final String BUCKET = "taninim-water";

    private static final String FLAC = ".flac";

    private static final String REGION = "eu-north-1";

    private static final String AMAZONAWS_COM = "https://s3.amazonaws.com/";

    public S3Streamer(IO io, Media media) {
        super(media, io, "/cloudio");
    }

    @Override
    protected HttpResponse stream(HttpRequest req, Track track, ChannelHandlerContext ctx) {
        UUID uuid = track.getUuid();
        HttpResponse response = response(req);
        String rangeHeader = req.headers().get(HttpHeaderNames.RANGE);

        MinioClient s3 = s3();

        long fileLength = length(s3, uuid);

        ChannelFuture sendFile = rangeHeader != null && rangeHeader.length() > 0
            ? writePartial(ctx, s3, uuid, fileLength, response, rangeHeader)
            : write(ctx, s3, uuid, fileLength, response);
        ChannelFuture lastContentFuture =
            ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

        if (!HttpUtil.isKeepAlive(req)) {
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }
        return response;
    }

    private long length(MinioClient s3, UUID uuid) {
        try {
            ObjectStat objectStat = s3.statObject(BUCKET,
                uuid + FLAC);
            return objectStat.length();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to stat " + uuid, e);
        }
    }

    private InputStream stream(MinioClient s3, UUID uuid, Long offset, Long length) {
        try {
            String obj = uuid.toString() + FLAC;
            return offset == null || length == null
                ? s3.getObject(BUCKET, obj)
                : s3.getObject(BUCKET, obj, offset, length);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load " + uuid, e);
        }
    }

    private MinioClient s3() {
        try {
            return new MinioClient(
                AMAZONAWS_COM,
                System.getProperty("awsKey"),
                System.getProperty("awsSecret"),
                REGION);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to connect to S3", e);
        }
    }

    private ChannelFuture write(
        ChannelHandlerContext ctx,
        MinioClient s3, UUID uuid,
        long length,
        HttpResponse response
    ) {
        HttpUtil.setContentLength(response, length);
        ByteBuf byteBuf = getByteBuf(s3, uuid, length);
        return ctx.writeAndFlush(byteBuf);
    }

    private ChannelFuture writePartial(
        ChannelHandlerContext ctx,
        MinioClient s3, UUID uuid,
        long length,
        HttpResponse response,
        String rangeHeader
    ) {
        PartialRequestInfo pri = getPartialRequestInfo(rangeHeader, length);
        updateHeaders(response, pri, length);
        ByteBuf buffer = getByteBuf(s3, uuid, pri);
        ctx.write(response);
        return ctx.writeAndFlush(buffer);
    }

    private ByteBuf getByteBuf(MinioClient s3, UUID uuid, long length) {
        InputStream object = stream(s3, uuid, null, null);
        ByteBuf buffer = Unpooled.buffer((int) length);
        try {
            int totalRead = 0;
            while (true) {
                totalRead += buffer.writeBytes(object, (int)length);
                if (totalRead >= length) {
                    return buffer;
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read from " + uuid, e);
        }
    }

    private ByteBuf getByteBuf(MinioClient s3, UUID uuid, PartialRequestInfo pri) {
        InputStream object = stream(s3, uuid, pri.getStartOffset(), pri.getEndOffset());
        long partialLength = pri.getEndOffset() - pri.getStartOffset();
        ByteBuf buffer = Unpooled.buffer((int)partialLength);
        try {
            buffer.writeBytes(object, (int)partialLength);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read from " + uuid, e);
        }
        return buffer;
    }
}
