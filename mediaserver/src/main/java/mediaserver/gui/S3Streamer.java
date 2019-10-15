package mediaserver.gui;

import io.minio.MinioClient;
import io.minio.ObjectStat;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedStream;
import mediaserver.files.Media;
import mediaserver.files.Track;
import mediaserver.util.IO;
import mediaserver.util.MostlyOnce;

import java.io.InputStream;
import java.util.UUID;
import java.util.function.Supplier;

public final class S3Streamer extends AbstractStreamer {

    private static final String BUCKET = "taninim-water";

    private static final String FLAC = ".flac";

    private static final String REGION = "eu-north-1";

    private static final String AMAZONAWS_COM = "https://s3.amazonaws.com/";

    private final Supplier<MinioClient> s3 = MostlyOnce.get(this::s3);

    public S3Streamer(IO io, Media media) {
        super(media, io, "/cloudio");
    }

    @Override
    protected HttpResponse stream(HttpRequest req, Track track, ChannelHandlerContext ctx) {
        UUID uuid = track.getUuid();
        HttpResponse response = response(req);
        String rangeHeader = req.headers().get(HttpHeaderNames.RANGE);

        long fileLength = length(uuid);

        ChannelFuture sendFile = rangeHeader != null && rangeHeader.length() > 0
            ? writePartial(ctx, uuid, fileLength, response, rangeHeader)
            : write(ctx, uuid, fileLength, response);
        ChannelFuture lastContentFuture =
            ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

        if (!HttpUtil.isKeepAlive(req)) {
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }
        return response;
    }

    private long length(UUID uuid) {
        try {
            ObjectStat objectStat = s3.get().statObject(BUCKET,
                uuid + FLAC);
            return objectStat.length();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to stat " + uuid, e);
        }
    }

    private InputStream stream(UUID uuid, Long offset, Long length) {
        try {
            String obj = uuid.toString() + FLAC;
            return offset == null || length == null
                ? s3.get().getObject(BUCKET, obj)
                : s3.get().getObject(BUCKET, obj, offset, length);
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
        UUID uuid,
        long length,
        HttpResponse response
    ) {
        HttpUtil.setContentLength(response, length);
        return writeData(ctx, response, null);// getByteBuf(uuid, length));
    }

    private ChannelFuture writeData(ChannelHandlerContext ctx, HttpResponse response, ByteBuf byteBuf) {
        ChannelFuture headers = ctx.write(response);
        if (byteBuf == null) {
            return headers;
        }
        ByteBufInputStream contentStream = new ByteBufInputStream(byteBuf);
        return ctx.writeAndFlush(
            new HttpChunkedInput(new ChunkedStream(contentStream)), ctx.newProgressivePromise());
    }

    private ChannelFuture writePartial(
        ChannelHandlerContext ctx,
        UUID uuid,
        long length,
        HttpResponse response,
        String rangeHeader
    ) {
        PartialRequestInfo pri = getPartialRequestInfo(rangeHeader, length);
        updateHeaders(response, pri, length);
        return writeData(ctx, response, getByteBuf(uuid, pri));
    }

    private ByteBuf getByteBuf(UUID uuid, long length) {
        InputStream object = stream(uuid, null, null);
        ByteBuf buffer = Unpooled.buffer((int) length);
        try {
            int totalRead = 0;
            while (true) {
                totalRead += buffer.writeBytes(object, (int) length);
                if (totalRead >= length) {
                    return buffer;
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read from " + uuid, e);
        }
    }

    private ByteBuf getByteBuf(UUID uuid, PartialRequestInfo pri) {
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
