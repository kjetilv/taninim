package mediaserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import mediaserver.files.Media;
import mediaserver.gui.*;
import mediaserver.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.function.Supplier;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static final int PORT = 8080;

    private static final String DEV_FLAG = "dev";

    public static void main(String[] args) {
        boolean local = args.length > 0 && new File(args[0]).isDirectory();
        boolean isDev = Boolean.getBoolean(DEV_FLAG);

        log.info("Running in {} mode", local ? "local" : "cloud");

        Media media = local ? Media.local(args[0]) : CloudMedia.download();

        EventLoopGroup listenGroup = new NioEventLoopGroup(1);
        EventLoopGroup workGroup = new NioEventLoopGroup(4);

        Supplier<Router> routerProvider = routerSupplier(media, local, isDev);

        ChannelInitializer<SocketChannel> handler = new ChannelInitializer<>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new CustomHttpServerCodec());
                pipeline.addLast(routerProvider.get());
            }
        };

        ServerBootstrap bootstrap = new ServerBootstrap()
            .group(listenGroup, workGroup)
            .channel(NioServerSocketChannel.class)
            .handler(new LoggingHandler(LogLevel.DEBUG))
            .childHandler(handler);

        try {
            Channel ch = bootstrap.bind(PORT).sync().channel();
            log.info("Bound to port {}", PORT);
            ch.closeFuture().sync();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted", e);
        } finally {
            listenGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }

    private static Supplier<Router> routerSupplier(
        Media media,
        boolean local,
        boolean dev
    ) {
        IO io = new IO(dev);
        return () -> new Router(new Playlists(io, media),
            local ? new FileStreamer(io, media) : new S3Streamer(io, media),
            new Resources(io),
            new GUI(io, media));
    }
}
