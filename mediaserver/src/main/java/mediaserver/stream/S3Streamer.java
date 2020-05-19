package mediaserver.stream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultHttpContent;
import mediaserver.externals.S3Client;
import mediaserver.externals.S3Connector;
import mediaserver.http.Route;
import mediaserver.media.Media;
import mediaserver.media.Track;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.util.function.Supplier;

public final class S3Streamer extends Streamer {

    private static final String M4A = "m4a";

    private static final String FLAC = "flac";

    private static final Logger log = LoggerFactory.getLogger(S3Streamer.class);

    private final S3Client client;

    public S3Streamer(Route route, Clock clock, Supplier<Media> media, S3Client client, int bytesPerChunk) {

        super(route, clock, media, bytesPerChunk);
        this.client = client;
    }

    @Override
    protected Object content(Track track, Chunk chunk, boolean lossless) {

        String type = type(lossless);
        ByteBuf byteBuf = data(track, type, chunk);
        return new DefaultHttpContent(byteBuf);
    }

    @Override
    protected long trackLength(Track track, boolean lossless) {

        return client
            .length(track.getUuid() + "." + type(lossless))
            .orElseThrow(() ->
                new IllegalStateException("Failed to assess length of " + track));
    }

    private static String type(boolean lossless) {

        return lossless ? FLAC : M4A;
    }

    private static ByteBuf data(Track track, String type, Chunk chunk) {

        ByteBuf buffer = Unpooled.buffer((int) chunk.getSize());
        try (InputStream input = jetStream(track, chunk.getStart(), chunk.getEnd(), type)) {
            long transferred = 0;
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
            log.warn("Failed to close cloud stream {} for {}/{}", chunk, track, type, e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read from " + track + "/" + type, e);
        }
        return buffer;
    }

    private static InputStream jetStream(Track track, Long offset, Long length, String type) {

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

    private static int intValue(long partialLength) {

        if (partialLength > Integer.MAX_VALUE) {
            throw new IllegalStateException("Unexpected length: " + partialLength);
        }
        return (int) partialLength;
    }
}
