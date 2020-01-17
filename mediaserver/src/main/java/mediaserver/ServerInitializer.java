package mediaserver;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

final class ServerInitializer extends ChannelInitializer<SocketChannel> {

    private final Router router;

    private final Duration ioTimeout;

    private final SslContext sslContext;

    ServerInitializer(Router router, Duration ioTimeout, SslContext sslContext) {

        this.router = router;
        this.ioTimeout = ioTimeout;
        this.sslContext = sslContext;
    }

    @Override
    protected void initChannel(SocketChannel ch) {

        ChannelPipeline pipeline = ch.pipeline();
        if (sslContext != null) {
            pipeline.addLast(sslContext.newHandler(ch.alloc()));
        }
        pipeline
            .addLast(new ReadTimeoutHandler(ioTimeout.toSeconds(), TimeUnit.SECONDS))
            .addLast(new WriteTimeoutHandler(ioTimeout.toSeconds(), TimeUnit.SECONDS))
            .addLast(new HttpServerCodec())
            .addLast(new HttpObjectAggregator(64 * 1024))
            .addLast(router);
    }
}
