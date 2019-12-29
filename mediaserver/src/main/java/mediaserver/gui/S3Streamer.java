package mediaserver.gui;

import io.minio.ObjectStat;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpResponse;
import mediaserver.http.WebPath;
import mediaserver.media.Media;
import mediaserver.media.Track;
import mediaserver.sessions.Session;
import mediaserver.sessions.Sessions;
import mediaserver.util.S3;

import java.io.InputStream;
import java.util.Optional;
import java.util.function.Supplier;

import static io.netty.handler.codec.http.HttpHeaderNames.RANGE;

public final class S3Streamer extends AbstractStreamer {

    public S3Streamer(Supplier<Media> media, Sessions sessions) {

        super(media, sessions);
    }

    @Override
    protected Optional<ChannelFuture> stream(
        WebPath webPath,
        Session session,
        Track track,
        boolean lossless,
        HttpResponse response,
        ChannelHandlerContext ctx
    ) {

        String type = lossless ? "flac" : "m4a";
        long fileLength = length(track, type);

        String rangeHeader = webPath.header(RANGE);
        if (rangeHeader == null || rangeHeader.length() <= 0) {
            return Optional.of(
                writeLength(ctx, response, fileLength));
        }

        Chunk chunk = chunk(rangeHeader, fileLength);
        ByteBuf byteBuf = read(track, type, chunk);
        DefaultHttpContent content = new DefaultHttpContent(byteBuf);

        return Optional.of(
            writeContent(session, ctx, chunk, response, content));
    }

    private long length(Track track, String type) {

        return S3.get().map(s3 -> {
            try {
                return s3.statObject(
                    S3.BUCKET,
                    track.getUuid() + "." + type);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to resolve in cloud: " + track + "/" + type, e);
            }
        }).map(ObjectStat::length)
            .orElseThrow(() ->
                new IllegalStateException("No S3 connection"));
    }

    private InputStream stream(Track track, Long offset, Long length, String type) {

        return S3.get().map(s3 -> {
            try {
                String obj = track.getUuid().toString() + "." + type;
                return offset == null || length == null
                    ? s3.getObject(S3.BUCKET, obj)
                    : s3.getObject(S3.BUCKET, obj, offset, length);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load " + track + "/" + type, e);
            }
        }).orElseThrow(() ->
            new IllegalStateException("No S3 connection"));
    }

    private ByteBuf read(Track track, String type, Chunk chunk) {

        InputStream input = stream(track, chunk.getStartOffset(), chunk.getEndOffset(), type);
        long partialLength = chunk.getEndOffset() - chunk.getStartOffset();
        ByteBuf buffer = Unpooled.buffer((int) partialLength);
        try {
            int totalRead = 0;
            while (true) {
                totalRead += buffer.writeBytes(input, intValue(partialLength));
                if (totalRead >= partialLength) {
                    return buffer;
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read from " + track + "/" + type, e);
        }
    }

    private int intValue(long partialLength) {

        if (partialLength > Integer.MAX_VALUE) {
            throw new IllegalStateException("Unexpected length: " + partialLength);
        }
        return (int) partialLength;
    }
}
