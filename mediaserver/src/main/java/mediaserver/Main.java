package mediaserver;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.ResourceLeakDetector;
import mediaserver.dto.AudioAlbum;
import mediaserver.files.Media;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Path;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);

        ObjectMapper objectMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        EventLoopGroup listenGroup = new NioEventLoopGroup(1);
        EventLoopGroup workGroup = new NioEventLoopGroup(4);

        Path root = Path.of(URI.create(
            "file://" + System.getProperty("user.home") + "/FLAC/John%20Zorn"));
        Media media = new Media(root);

        ServerBootstrap bootstrap = new ServerBootstrap()
            .group(listenGroup, workGroup)
            .channel(NioServerSocketChannel.class)
            .handler(new LoggingHandler(LogLevel.INFO))
            .childHandler(new MediaServerInitializer(() ->
                new MediaServerRouter(new DirectoryLister(root, media, objectMapper))
            ));
        try {
            Channel ch = bootstrap.bind(8080).sync().channel();
            ch.closeFuture().sync();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted", e);
        } finally {
            listenGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }

    private static byte[] write(ObjectMapper objectMapper, AudioAlbum directory) {
        try {
            return objectMapper.writeValueAsBytes(directory);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to write " + directory, e);
        }
    }
}
