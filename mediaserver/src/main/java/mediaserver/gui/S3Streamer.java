package mediaserver.gui;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpResponse;
import mediaserver.http.WebPath;
import mediaserver.media.Media;
import mediaserver.media.Track;
import mediaserver.util.S3;
import mediaserver.util.S3Connector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Supplier;

import static io.netty.handler.codec.http.HttpHeaderNames.RANGE;

public final class S3Streamer extends AbstractStreamer {

    private static final Logger log = LoggerFactory.getLogger(S3Streamer.class);

    private final S3.Client client;

    public static final String M4A = "m4a";

    public static final String FLAC = "flac";

    public S3Streamer(Supplier<Media> media, S3.Client client, int bytesPerChunk) {

        super(media, bytesPerChunk);
        this.client = client;
    }

    @Override
    protected Optional<ChannelFuture> streamFuture(
        WebPath webPath,
        Track track,
        boolean lossless,
        HttpResponse response
    ) {

        String type = lossless ? FLAC : M4A;
        return length(track, type).flatMap(length ->
            BytesRange.read(webPath.header(RANGE))
                .filter(bytesRange ->
                    bytesRange.isSatisfiable(length))
                .map(bytesRange -> {
                    Chunk chunk = chunk(bytesRange, length);
                    ByteBuf byteBuf = data(track, type, chunk);
                    Object content = new DefaultHttpContent(byteBuf);
                    return respondData(webPath, response, chunk, content);
                }));
    }

    @Override
    protected long length(Track track, boolean lossless) {

        return length(track, lossless ? "flac" : "m4a").orElseThrow(() ->
            new IllegalStateException("Failed to assess length of " + track));
    }

    private Optional<Long> length(Track track, String type) {

        return client.length(track.getUuid() + "." + type);
    }

    private ByteBuf data(Track track, String type, Chunk chunk) {

        ByteBuf buffer = Unpooled.buffer((int) chunk.getSize());
        try (InputStream input = stream(track, chunk.getStart(), chunk.getEnd(), type)) {
            int transferred = 0;
            while (true) {
                try {
                    transferred += buffer.writeBytes(input, intValue(chunk.getSize()));
                } catch (Exception e) {
                    throw new IllegalStateException(
                        "Failed to stream after " + transferred + " bytes: " + chunk, e);
                }
                if (transferred >= chunk.getSize()) {
                    return buffer;
                }
            }
        } catch (IOException e) {
            log.warn("Failed to close", e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read from " + track + "/" + type, e);
        }
        return buffer;
    }

    private InputStream stream(Track track, Long offset, Long length, String type) {

        return S3Connector.get().map(s3 -> {
            try {
                return s3.stream(
                    track.getUuid().toString() + "." + type,
                    offset,
                    length);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load " + track + "/" + type, e);
            }
        }).orElseThrow(() ->
            new IllegalStateException("No S3 connection"));
    }

    private int intValue(long partialLength) {

        if (partialLength > Integer.MAX_VALUE) {
            throw new IllegalStateException("Unexpected length: " + partialLength);
        }
        return (int) partialLength;
    }
}
