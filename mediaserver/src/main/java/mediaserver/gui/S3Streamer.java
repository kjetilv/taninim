package mediaserver.gui;

import io.minio.ObjectStat;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import mediaserver.Media;
import mediaserver.files.Track;
import mediaserver.sessions.Sessions;
import mediaserver.util.IO;
import mediaserver.util.S3;

import java.io.InputStream;
import java.util.UUID;
import java.util.function.Supplier;

public final class S3Streamer extends AbstractStreamer {

    public S3Streamer(IO io, Supplier<Media> media, Sessions sessions) {

        super(io, media, sessions);
    }

    @Override
    protected ChannelFuture stream(HttpRequest req, Track track, ChannelHandlerContext ctx, HttpResponse res) {
        UUID uuid = track.getUuid();
        String audioType = audioType(req);
        long fileLength = length(uuid, audioType);

        String rangeHeader = req.headers().get(HttpHeaderNames.RANGE);
        if (rangeHeader == null || rangeHeader.length() <= 0) {
            return writeLength(ctx, res, fileLength);
        }
        return writePartial(ctx, uuid, audioType, fileLength, res, rangeHeader)
            .addListener(new ProgressListener(uuid));
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

    private ChannelFuture writePartial(
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
        return ctx.writeAndFlush(
            new DefaultHttpContent(byteBuf),
            ctx.newProgressivePromise());
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
