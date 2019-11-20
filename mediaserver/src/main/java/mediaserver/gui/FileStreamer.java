package mediaserver.gui;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import mediaserver.Media;
import mediaserver.files.Track;
import mediaserver.sessions.Sessions;
import mediaserver.util.IO;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.function.Supplier;

public final class FileStreamer extends AbstractStreamer {

    public FileStreamer(IO io, Supplier<Media> media, Sessions sessions) {

        super(io, media, sessions);
    }

    @Override
    protected ChannelFuture stream(HttpRequest req, Track track, ChannelHandlerContext ctx, HttpResponse res) {

        File file = isFlac(req) ? track.getFile() : track.getCompressedFile();
        RandomAccessFile randomAccessFile = randomAccess(file);
        long fileLength = length(randomAccessFile);

        String rangeHeader = req.headers().get(HttpHeaderNames.RANGE);
        if (rangeHeader == null || rangeHeader.length() == 0) {
            return writeLength(ctx, res, fileLength);
        }
        return writeRange(res, fileLength, ctx, randomAccessFile, rangeHeader.trim())
            .addListener(progressListener(track));
    }

    private static ProgressListener progressListener(Track track) {

        return new ProgressListener(
            track.getArtist().getName() + ": " + track.getName() + " [" + track.getAlbum() + "]");
    }

    private static RandomAccessFile randomAccess(File file) {

        try {
            return new RandomAccessFile(file, "r");
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Expected to find file " + file, e);
        }
    }

    private static ChannelFuture writeRange(
        HttpResponse response, long length, ChannelHandlerContext ctx,
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
        } catch (IOException e) {
            throw new IllegalStateException("Could not read length of " + randomAccessFile, e);
        }
    }
}
