package mediaserver.gui;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.*;
import mediaserver.files.Media;
import mediaserver.files.Track;
import mediaserver.util.IO;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public final class FileStreamer extends AbstractStreamer {

    public FileStreamer(IO io, Media media) {
        super(media, io, "/audio");
    }

    @Override
    protected HttpResponse stream(HttpRequest req, Track track, ChannelHandlerContext ctx) {
        HttpResponse response = response(req);

        File file = track.getFile();
        RandomAccessFile randomAccessFile = randomAccess(file);
        long fileLength = length(randomAccessFile);

        String rangeHeader = req.headers().get(HttpHeaderNames.RANGE);
        if (rangeHeader != null && rangeHeader.length() > 0) {
            writePartial(response, fileLength, ctx, randomAccessFile, rangeHeader.trim());
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

    private static ChannelFuture writePartial(
        HttpResponse response, long length, ChannelHandlerContext ctx,
        RandomAccessFile file,
        String rangeHeader
    ) {
        PartialRequestInfo pri = getPartialRequestInfo(rangeHeader, length);
        updateHeaders(response, pri, length);
        return writeData(ctx, file, response, pri.getStartOffset(), pri.getChunkSize())
            .addListener(new ProgressListener(file));
    }

    private static ChannelFuture writeData(
        ChannelHandlerContext ctx,
        RandomAccessFile file,
        HttpResponse response,
        long startOffset,
        long chunkSize
    ) {
        ctx.write(response);
        return ctx.write(
            new DefaultFileRegion(file.getChannel(), startOffset, chunkSize),
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
