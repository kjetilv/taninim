package mediaserver.gui;

import io.netty.channel.DefaultFileRegion;
import mediaserver.media.Media;
import mediaserver.media.Track;
import mediaserver.toolkit.Chunk;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.time.Clock;
import java.util.function.Supplier;

public final class FileStreamer extends Streamer {

    public FileStreamer(Clock clock, Supplier<Media> media, int bytesPerChunk) {

        super(clock, media, bytesPerChunk);
    }

    @Override
    protected Object content(Track track, Chunk chunk, boolean lossless) {
        File file = lossless ? track.getFile() : track.getCompressedFile();
        FileChannel channel = randomAccess(file).getChannel();
        return new DefaultFileRegion(channel, chunk.getStart(), chunk.getSize());
    }

    @Override
    protected long trackLength(Track track, boolean lossless) {

        return length(lossless ? track.getFile() : track.getCompressedFile());
    }

    private static RandomAccessFile randomAccess(File file) {

        try {
            return new RandomAccessFile(file, "r");
        } catch (Exception e) {
            throw new IllegalStateException("Expected to find file " + file, e);
        }
    }

    private static long length(File file) {

        try {
            return file.length();
        } catch (Exception e) {
            throw new IllegalStateException("Could not read length of " + file, e);
        }
    }
}
