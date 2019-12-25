package mediaserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

final class NettyRun {

    private static final Logger log = LoggerFactory.getLogger(NettyRun.class);

    private final EventLoopGroup listenGroup;

    private final EventLoopGroup workGroup;

    NettyRun(int work, int listen) {

        workGroup = new NioEventLoopGroup(work);
        listenGroup = new NioEventLoopGroup(listen);
    }

    void run(
        Supplier<Router> routerProvider,
        @SuppressWarnings("SameParameterValue") int port,
        SslContext sslContext
    ) {
        try {
            ChannelInitializer<SocketChannel> handler =
                new ServerInitializer(routerProvider, sslContext);
            ServerBootstrap bootstrap =
                bootstrap(listenGroup, workGroup, handler);
            Channel ch =
                bootstrap.bind(port).sync().channel();
            log.info("Bound to port {}: {}", port, ch);

            registerCloser(ch);

            ChannelFuture syn = ch.closeFuture().sync();
            log.info("Port {} dropped: {}", port, syn);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted", e);
        } finally {
            listenGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }

    private void registerCloser(Channel ch) {

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Closing {} ...", ch);
            ch.close();
        }, "Closer"));
    }

    private static ServerBootstrap bootstrap(
        EventLoopGroup listenGroup,
        EventLoopGroup workGroup,
        ChannelInitializer<SocketChannel> handler
    ) {

        return new ServerBootstrap()
            .group(listenGroup, workGroup)
            .channel(NioServerSocketChannel.class)
            .handler(new LoggingHandler(LogLevel.DEBUG))
            .childHandler(handler);
    }
}
