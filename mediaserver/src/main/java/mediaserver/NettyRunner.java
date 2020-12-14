package mediaserver;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS;
import static java.lang.StrictMath.toIntExact;

@SuppressWarnings("SameParameterValue")
final class NettyRunner {

    private static final Logger log = LoggerFactory.getLogger(NettyRunner.class);

    private final int listenThreads;

    private final int workThreads;

    private final int threads;

    private final int queue;

    private final Duration timeout;

    NettyRunner(int listenThreads, int workThreads, int threads, int queue, Duration timeout) {
        this.listenThreads = listenThreads;
        this.workThreads = workThreads;
        this.threads = threads;
        this.queue = queue;
        this.timeout = timeout;
    }

    void run(Router router, int port, SslContext sslContext) {
        EventLoopGroup listen = listenGroup();
        EventLoopGroup work = workGroup();
        try {
            ChannelFuture opened = sync(
                future(router, port, sslContext, listen, work),
                "Interrupted while starting");
            registerShutdownClosure(opened);
            log.info("{}: Bound to port {}: {}", this, port, opened.channel());
            ChannelFuture closed = sync(
                opened.channel().closeFuture(),
                "Interrupted while closing");
            log.info("{}: Unstuck from {}: {}", this, port, closed);
        } finally {
            listen.shutdownGracefully();
            work.shutdownGracefully();
        }
    }

    private void registerShutdownClosure(ChannelFuture sync) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("{}: Closing {} ...", this, sync.channel());
            sync.channel().close();
        }, "Closer"));
    }

    private EventLoopGroup listenGroup() {
        return new NioEventLoopGroup(this.listenThreads, countingThreadFactory("lst"));
    }

    private EventLoopGroup workGroup() {
        return new NioEventLoopGroup(this.workThreads, new ThreadPoolExecutor(
            threads,
            threads,
            60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(queue),
            countingThreadFactory("thr"),
            new ThreadPoolExecutor.CallerRunsPolicy()));
    }

    private ChannelFuture future(
        Router router,
        int port,
        SslContext sslContext,
        EventLoopGroup listenGroup,
        EventLoopGroup workGroup
    ) {
        return new ServerBootstrap()
            .option(CONNECT_TIMEOUT_MILLIS, toIntExact(timeout.toMillis()))
            .group(listenGroup, workGroup)
            .channel(NioServerSocketChannel.class)
            .handler(new LoggingHandler())
            .childHandler(new ChannelInit(sslContext, router))
            .bind(port);
    }

    private static ChannelFuture sync(ChannelFuture future, String msg) {
        try {
            return future.sync();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(msg + ": " + future, e);
        }
    }

    private static ThreadFactory countingThreadFactory(String prefix) {
        LongAdder count = new LongAdder();
        return r -> {
            try {
                return new Thread(r, prefix + '#' + count.longValue());
            } finally {
                count.increment();
            }
        };
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" +
            "l:" + listenThreads + "/w:" + workThreads +
            " -> " +
            "t:" + threads + "/q:" + queue +
            "]";
    }
}
