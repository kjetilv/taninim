package mediaserver;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;

final class ChannelInit extends ChannelInitializer<NioSocketChannel> {

    private final SslContext sslContext;

    private final Router router;

    ChannelInit(SslContext sslContext, Router router) {
        this.sslContext = sslContext;
        this.router = router;
    }

    @Override
    protected void initChannel(NioSocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        if (sslContext != null) {
            pipeline.addLast(sslContext.newHandler(ch.alloc()));
        }
        pipeline
            .addLast("decoder", new HttpServerCodec())
            .addLast("aggregator", new HttpObjectAggregator(Config.BYTES_PER_CHUNK * 2))
            .addLast(router);
    }
}
