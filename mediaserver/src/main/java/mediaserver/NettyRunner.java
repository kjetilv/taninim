package mediaserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

import static io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS;

@SuppressWarnings("SameParameterValue")
final class NettyRunner {

    private static final Logger log = LoggerFactory.getLogger(NettyRunner.class);

    private final int listenGroup;

    private final int workGroup;

    private final int threadGroup;

    private final int threadQueue;

    private final Duration ioTimeout;

    private final Duration connectTimeout;

    NettyRunner(
        int listenGroup,
        int workGroup,
        int threadGroup,
        int threadQueue,
        Duration ioTimeout,
        Duration connectTimeout
    ) {

        this.listenGroup = listenGroup;
        this.workGroup = workGroup;
        this.threadGroup = threadGroup;
        this.threadQueue = threadQueue;
        this.ioTimeout = ioTimeout;
        this.connectTimeout = connectTimeout;
    }

    void run(Router router, int port, SslContext sslContext) {

        EventLoopGroup listenGroup = new NioEventLoopGroup(
            this.listenGroup,
            countingThreadFactory("lst"));

        Executor workExecutor = new ThreadPoolExecutor(threadGroup, threadGroup, 60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(threadQueue),
            countingThreadFactory("thr"),
            new ThreadPoolExecutor.CallerRunsPolicy());

        EventLoopGroup workGroup = new NioEventLoopGroup(this.workGroup, workExecutor);

        try {
            Channel channel = new ServerBootstrap()
                .option(
                    CONNECT_TIMEOUT_MILLIS, (int) connectTimeout.toMillis())
                .group(
                    listenGroup, workGroup)
                .channel(
                    NioServerSocketChannel.class)
                .handler(
                    new LoggingHandler())
                .childHandler(
                    new ServerInitializer(router, ioTimeout, sslContext))
                .bind(port)
                .sync()
                .channel();

            log.info("{}: Bound to port {}: {}", this, port, channel);
            registerCloser(channel);
            ChannelFuture closed = channel.closeFuture().sync();
            log.info("{}: Became unstuck from {}: {}", this, port, closed);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted", e);
        } finally {
            listenGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }

    private ThreadFactory countingThreadFactory(String prefix) {

        LongAdder count = new LongAdder();
        return r -> {
            try {
                return new Thread(r, prefix + '#' + count.longValue());
            } finally {
                count.increment();
            }
        };
    }

    private void registerCloser(Channel ch) {

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("{}: Closing {} ...", this, ch);
            ch.close();
        }, "Closer"));
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() +
            "[listenGroup=" + listenGroup +
            " workGroup=" + workGroup +
            " threadGroup=" + threadGroup +
            " threadQueue=" + threadQueue +
            " ioTimeout=" + ioTimeout +
            " connectTimeout=" + connectTimeout +
            "]";
    }
}
