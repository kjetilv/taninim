package mediaserver.stream;

import io.netty.channel.DefaultFileRegion;
import mediaserver.http.Route;
import mediaserver.media.Media;
import mediaserver.media.Track;

import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.time.Clock;
import java.util.function.Supplier;

public final class FileStreamer extends Streamer {

    public FileStreamer(Route route, Clock clock, Supplier<Media> media, int bytesPerChunk) {

        super(route, clock, media, bytesPerChunk);
    }

    @Override
    protected Object content(Track track, Chunk chunk, boolean lossless) {

        Path file = lossless ? track.getFile() : track.getCompressedFile();
        FileChannel channel = randomAccess(file).getChannel();
        return new DefaultFileRegion(channel, chunk.getStart(), chunk.getSize());
    }

    @Override
    protected long trackLength(Track track, boolean lossless) {

        return lossless ? track.getFileSize() : track.getCompressedSize();
    }

    private static RandomAccessFile randomAccess(Path file) {

        try {
            return new RandomAccessFile(file.toFile(), "r");
        } catch (Exception e) {
            throw new IllegalStateException("Expected to find file " + file, e);
        }
    }
}
