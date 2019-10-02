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

import java.net.URI;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);

        ObjectMapper objectMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        EventLoopGroup listenGroup = new NioEventLoopGroup(1);
        EventLoopGroup workGroup = new NioEventLoopGroup(4);

        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                .group(listenGroup, workGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new MediaServerInitializer(
                    () -> new MediaServerRouter(new DirectoryLister(
                        Path.of(URI.create(
                            "file://" + System.getProperty("user.home") + "/FLAC/John%20Zorn")),
                        directory -> {
                            try {
                                return objectMapper.writeValueAsBytes(directory);
                            } catch (JsonProcessingException e) {
                                throw new IllegalStateException("Failed to write " + directory, e);
                            }
                        }))
                ));
            Channel ch = bootstrap.bind(8080).sync().channel();
            ch.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            listenGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }
}
