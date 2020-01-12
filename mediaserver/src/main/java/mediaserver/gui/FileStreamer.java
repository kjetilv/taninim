package mediaserver.gui;

import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.http.HttpResponse;
import mediaserver.http.WebPath;
import mediaserver.media.Media;
import mediaserver.media.Track;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Optional;
import java.util.function.Supplier;

import static io.netty.handler.codec.http.HttpHeaderNames.RANGE;

public final class FileStreamer extends AbstractStreamer {

    public FileStreamer(Supplier<Media> media, int bytesPerChunk) {

        super(media, bytesPerChunk);
    }

    @Override
    protected Optional<ChannelFuture> streamFuture(
        WebPath webPath,
        Track track,
        boolean lossless,
        HttpResponse response
    ) {

        long fileLength = length(track, lossless);

        return BytesRange.read(webPath.header(RANGE))
            .filter(bytesRange ->
                bytesRange.isSatisfiable(fileLength))
            .map(bytesRange -> {
                Chunk chunk = chunk(bytesRange, fileLength);
                FileChannel channel = randomAccess(lossless ? track.getFile() : track.getCompressedFile()).getChannel();
                FileRegion content =
                    new DefaultFileRegion(channel, chunk.getStart(), chunk.getSize());
                return respondData(webPath, response, chunk, content);
            });
    }

    @Override
    protected long length(Track track, boolean lossless) {

        return length(lossless ? track.getFile() : track.getCompressedFile());
    }

    private static RandomAccessFile randomAccess(File file) {

        try {
            return new RandomAccessFile(file, "r");
        } catch (Exception e) {
            throw new IllegalStateException("Expected to find file " + file, e);
        }
    }

    private static long length(RandomAccessFile randomAccessFile) {

        try {
            return randomAccessFile.length();
        } catch (Exception e) {
            throw new IllegalStateException("Could not read length of " + randomAccessFile, e);
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
