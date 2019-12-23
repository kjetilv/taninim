package mediaserver;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;

import java.util.function.Supplier;

final class ServerInitializer extends ChannelInitializer<SocketChannel> {

    private final Supplier<Router> routerProvider;

    private final SslContext sslContext;

    ServerInitializer(Supplier<Router> routerProvider, SslContext sslContext) {

        this.routerProvider = routerProvider;
        this.sslContext = sslContext;
    }

    @Override
    protected void initChannel(SocketChannel ch) {

        ChannelPipeline pipeline = ch.pipeline();
        if (sslContext != null) {
            pipeline.addLast(sslContext.newHandler(ch.alloc()));
        }
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(64 * 1024));
        pipeline.addLast(routerProvider.get());
    }
}
