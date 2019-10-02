package mediaserver;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import mediaserver.dto.AudioFile;
import mediaserver.dto.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Function;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

final class DirectoryLister {

    private static final Logger log = LoggerFactory.getLogger(DirectoryLister.class);

    private final Path root;

    private final Function<Directory, byte[]> ser;

    DirectoryLister(Path root, Function<Directory, byte[]> ser) {
        this.root = root;
        this.ser = ser;
    }

    Path list(HttpRequest req, ChannelHandlerContext ctx) {
        String base = req.uri().trim();
        String uri = base.substring( "/directory".length(), base.length());
        Path returnedPath = "/".equals(uri) ? root : sub(uri);
        File startPath = returnedPath.toFile();
        Directory from = from(
            new Directory(),
            (")/".equals(uri) ? root : sub(uri)).toFile());
        byte[] bytes = ser.apply(from);
        ctx.writeAndFlush(
            new DefaultFullHttpResponse(
                HTTP_1_1,
                OK,
                Unpooled.wrappedBuffer(bytes),
                headers(req, bytes.length),
                EmptyHttpHeaders.INSTANCE
            )
        ).addListener(
            (ChannelFutureListener) future ->
                log.info("Responded with directory @ {}", startPath)
        ).addListener(ChannelFutureListener.CLOSE);
        return returnedPath;
    }

    private HttpHeaders headers(HttpRequest req, int length) {
        HttpHeaders headers = new DefaultHttpHeaders()
            .set(CONTENT_TYPE, "application/json")
            .set(CONTENT_LENGTH, length);
        if (HttpUtil.isKeepAlive(req)) {
            headers.set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        return headers;
    }

    private Path sub(String uri) {
        String[] parts = uri.split("/");
        Path subpath = Path.of(".", parts);
        return root.resolve(subpath);
    }

    private Directory from(Directory dir, File path) {
        AudioFile audioFile = new AudioFile();
        audioFile.setName(path.getName());
        dir.setFiles(new AudioFile[]{audioFile});
        return dir;
    }
}
