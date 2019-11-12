package mediaserver.gui;

import io.minio.ObjectStat;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import mediaserver.Media;
import mediaserver.files.Track;
import mediaserver.util.IO;
import mediaserver.util.S3;

import java.io.InputStream;
import java.util.UUID;

public final class S3Streamer extends AbstractStreamer {

    public S3Streamer(IO io, Media media) {

        super(io, media);
    }

    @Override
    protected HttpResponse stream(HttpRequest req, Track track, ChannelHandlerContext ctx) {

        UUID uuid = track.getUuid();
        HttpResponse response = response(req);
        String rangeHeader = req.headers().get(HttpHeaderNames.RANGE);

        String audioType = audioType(req);
        long fileLength = length(uuid, audioType);

        if (rangeHeader != null && rangeHeader.length() > 0) {
            writePartial(ctx, uuid, audioType, fileLength, response, rangeHeader);
        } else {
            HttpUtil.setContentLength(response, fileLength);
            ctx.write(response);
        }

        ChannelFuture lastContentFuture =
            ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        if (!HttpUtil.isKeepAlive(req)) {
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }
        return response;
    }

    private long length(UUID uuid, String type) {

        return S3.get().map(s3 -> {
            try {
                return s3.statObject(
                    S3.BUCKET,
                    uuid + "." + type);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to stat " + uuid, e);
            }
        }).map(ObjectStat::length)
            .orElseThrow(() ->
                new IllegalStateException("No S3 connection"));
    }

    private InputStream stream(UUID uuid, Long offset, Long length, String type) {

        return S3.get().map(s3 -> {
            try {
                String obj = uuid.toString() + "." + type;
                return offset == null || length == null
                    ? s3.getObject(S3.BUCKET, obj)
                    : s3.getObject(S3.BUCKET, obj, offset, length);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load " + uuid, e);
            }
        }).orElseThrow(() ->
            new IllegalStateException("No S3 connection"));
    }

    private void writePartial(
        ChannelHandlerContext ctx,
        UUID uuid,
        String type,
        long length,
        HttpResponse response,
        String rangeHeader
    ) {

        PartialRequestInfo pri = getPartialRequestInfo(rangeHeader, length);
        ByteBuf byteBuf = read(uuid, type, pri);
        updateHeaders(response, pri, length);
        ctx.write(response);
        ctx.writeAndFlush(new DefaultHttpContent(byteBuf), ctx.newProgressivePromise())
            .addListener(new ProgressListener(uuid));
    }

    private ByteBuf read(UUID uuid, String type, PartialRequestInfo pri) {

        InputStream object = stream(uuid, pri.getStartOffset(), pri.getEndOffset(), type);
        long partialLength = pri.getEndOffset() - pri.getStartOffset();
        ByteBuf buffer = Unpooled.buffer((int) partialLength);
        try {
            int totalRead = 0;
            while (true) {
                totalRead += buffer.writeBytes(object, (int) partialLength);
                if (totalRead >= partialLength) {
                    return buffer;
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read from " + uuid, e);
        }
    }
}
