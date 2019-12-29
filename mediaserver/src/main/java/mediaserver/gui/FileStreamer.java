package mediaserver.gui;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.HttpResponse;
import mediaserver.http.WebPath;
import mediaserver.media.Media;
import mediaserver.media.Track;
import mediaserver.sessions.Session;
import mediaserver.sessions.Sessions;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Optional;
import java.util.function.Supplier;

import static io.netty.handler.codec.http.HttpHeaderNames.RANGE;

public final class FileStreamer extends AbstractStreamer {

    public FileStreamer(Supplier<Media> media, Sessions sessions) {

        super(media, sessions);
    }

    @Override
    protected Optional<ChannelFuture> streamFuture(
        WebPath webPath,
        Session session,
        Track track,
        boolean lossless,
        HttpResponse response,
        ChannelHandlerContext ctx
    ) {

        File sourceFile = lossless
            ? track.getFile()
            : track.getCompressedFile();
        RandomAccessFile file = randomAccess(sourceFile);
        long fileLength = length(file);

        String rangeHeader = webPath.header(RANGE);
        if (rangeHeader == null || rangeHeader.length() == 0) {
            return Optional.of(
                writeLength(ctx, response, fileLength));
        }

        Chunk chunk = chunk(rangeHeader.trim(), fileLength);
        FileChannel channel = file.getChannel();
        DefaultFileRegion content = new DefaultFileRegion(
            channel, chunk.getStartOffset(), chunk.getSize());

        return Optional.of(
            writeContent(session, ctx, chunk, response, content));
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
}
