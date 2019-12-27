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
import java.util.Optional;
import java.util.function.Supplier;

import static io.netty.handler.codec.http.HttpHeaderNames.RANGE;

public final class FileStreamer extends AbstractStreamer {

    public FileStreamer(Supplier<Media> media, Sessions sessions) {

        super(media, sessions);
    }

    @Override
    protected Optional<ChannelFuture> stream(
        WebPath webPath,
        Session user,
        Track track,
        boolean lossless,
        HttpResponse response,
        ChannelHandlerContext ctx
    ) {

        File file = lossless ? track.getFile() : track.getCompressedFile();
        RandomAccessFile randomAccessFile = randomAccess(file);
        long fileLength = length(randomAccessFile);

        String rangeHeader = webPath.header(RANGE);
        if (rangeHeader == null || rangeHeader.length() == 0) {
            ChannelFuture fullFuture =
                writeLength(ctx, response, fileLength);
            return Optional.of(fullFuture);
        }
        ChannelFuture partialFuture =
            writePartial(response, fileLength, ctx, randomAccessFile, rangeHeader.trim());
        return Optional.of(partialFuture);
    }

    private static RandomAccessFile randomAccess(File file) {

        try {
            return new RandomAccessFile(file, "r");
        } catch (Exception e) {
            throw new IllegalStateException("Expected to find file " + file, e);
        }
    }

    private static ChannelFuture writePartial(
        HttpResponse response,
        long length,
        ChannelHandlerContext ctx,
        RandomAccessFile file,
        String rangeHeader
    ) {

        PartialRequestInfo pri = getPartialRequestInfo(rangeHeader, length);
        updateHeaders(response, pri, length);
        ctx.write(response);
        return ctx.write(
            new DefaultFileRegion(file.getChannel(), pri.getStartOffset(), pri.getChunkSize()),
            ctx.newProgressivePromise());
    }

    private static long length(RandomAccessFile randomAccessFile) {

        try {
            return randomAccessFile.length();
        } catch (Exception e) {
            throw new IllegalStateException("Could not read length of " + randomAccessFile, e);
        }
    }
}
