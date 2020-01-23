package mediaserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
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
                    new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) {

                            ChannelPipeline pipeline = ch.pipeline();
                            if (sslContext != null) {
                                pipeline.addLast(sslContext.newHandler(ch.alloc()));
                            }
                            init(pipeline, router);
                        }
                    })
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

    private void init(ChannelPipeline channelPipeline, Router router) {

        channelPipeline
            .addLast("decoder", new HttpServerCodec())
            .addLast("aggregator", new HttpObjectAggregator(Config.BYTES_PER_CHUNK * 2) {

                @Override
                protected void handleOversizedMessage(ChannelHandlerContext ctx, HttpMessage oversized) {

                    log.warn("{}: Oversized message: {}", ctx, oversized);
                }
            })
            .addLast(router);
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
