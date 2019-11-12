package mediaserver.gui;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.*;
import mediaserver.Media;
import mediaserver.files.Track;
import mediaserver.util.IO;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public final class FileStreamer extends AbstractStreamer {

    public FileStreamer(IO io, Media media) {

        super(io, media);
    }

    @Override
    protected HttpResponse stream(HttpRequest req, Track track, ChannelHandlerContext ctx) {

        HttpResponse response = response(req);

        File file = isFlac(req) ? track.getFile() : track.getCompressedFile();
        RandomAccessFile randomAccessFile = randomAccess(file);
        long fileLength = length(randomAccessFile);

        String rangeHeader = req.headers().get(HttpHeaderNames.RANGE);
        if (rangeHeader != null && rangeHeader.length() > 0) {
            writeRange(response, fileLength, ctx, randomAccessFile, rangeHeader.trim())
                .addListener(new ProgressListener(
                    track.getArtist().getName() + ": " + track.getName() + " [" + track.getAlbum() + "]"));
        } else {
            HttpUtil.setContentLength(response, fileLength);
            ctx.write(response);
        }

        ChannelFuture lastContentFuture =
            ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        if (!HttpUtil.isKeepAlive(req)) {
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }
        return response;
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
