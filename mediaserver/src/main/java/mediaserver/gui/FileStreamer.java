package mediaserver.gui;

import io.netty.channel.*;
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
        File file = track.getFile();
        RandomAccessFile randomAccessFile = randomAccess(file);
        long fileLength = length(randomAccessFile);

        HttpResponse response = response(req);

        String rangeHeader = req.headers().get(HttpHeaderNames.RANGE);

        ChannelFuture sendFile = rangeHeader != null && rangeHeader.length() > 0
            ? writePartial(ctx, randomAccessFile, fileLength, response, rangeHeader)
            : write(ctx, randomAccessFile, fileLength, response);

        ChannelFuture lastContentFuture =
            ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

        sendFile.addListener(new ProgressListener(file));

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

    private static ChannelFuture write(
        ChannelHandlerContext ctx,
        RandomAccessFile file,
        long len,
        HttpResponse response
    ) {
        HttpUtil.setContentLength(response, len);
        ctx.write(response);
        return ctx.write(
            new DefaultFileRegion(file.getChannel(), 0, len),
            ctx.newProgressivePromise());
    }

    private static ChannelFuture writePartial(
        ChannelHandlerContext ctx,
        RandomAccessFile file,
        long length,
        HttpResponse response,
        String rangeHeader
    ) {
        PartialRequestInfo pri = getPartialRequestInfo(rangeHeader, length);
        updateHeaders(response, pri, length);

        ctx.write(response);

        FileRegion msg = fileRegion(file, pri);
        return ctx.write(msg, ctx.newProgressivePromise());
    }

    private static FileRegion fileRegion(RandomAccessFile file, PartialRequestInfo pri) {
        return new DefaultFileRegion(file.getChannel(), pri.getStartOffset(), pri.getChunkSize());
    }

    private static long length(RandomAccessFile randomAccessFile) {
        try {
            return randomAccessFile.length();
        } catch (IOException e) {
            throw new IllegalStateException("Could not read length of " + randomAccessFile, e);
        }
    }
}
