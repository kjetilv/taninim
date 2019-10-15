package mediaserver.gui;

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
import mediaserver.util.S3;

import java.io.InputStream;
import java.util.UUID;

public final class S3Streamer extends AbstractStreamer {

    private static final String FLAC = ".flac";

    public S3Streamer(IO io, Media media) {
        super(media, io);
    }

    @Override
    protected HttpResponse stream(HttpRequest req, Track track, ChannelHandlerContext ctx) {
        UUID uuid = track.getUuid();
        HttpResponse response = response(req);
        String rangeHeader = req.headers().get(HttpHeaderNames.RANGE);

        long fileLength = length(uuid);
        if (rangeHeader != null && rangeHeader.length() > 0) {
            writePartial(ctx, uuid, fileLength, response, rangeHeader);
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

    private long length(UUID uuid) {
        return S3.get().map(s3 -> {
            try {
                return s3.statObject(S3.BUCKET,
                    uuid + FLAC);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to stat " + uuid, e);
            }
        }).map(ObjectStat::length)
            .orElseThrow(() ->
                new IllegalStateException("No S3 connection"));
    }

    private InputStream stream(UUID uuid, Long offset, Long length) {
        return S3.get().map(s3 -> {
            try {
                String obj = uuid.toString() + FLAC;
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
        long length,
        HttpResponse response,
        String rangeHeader
    ) {
        PartialRequestInfo pri = getPartialRequestInfo(rangeHeader, length);
        ByteBuf byteBuf = read(uuid, pri);
        updateHeaders(response, pri, length);
        ctx.write(response);
        return ctx.writeAndFlush(
            new DefaultHttpContent(byteBuf), ctx.newProgressivePromise()
        ).addListener(new ProgressListener(uuid));
    }

    private ByteBuf read(UUID uuid, PartialRequestInfo pri) {
        InputStream object = stream(uuid, pri.getStartOffset(), pri.getEndOffset());
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
