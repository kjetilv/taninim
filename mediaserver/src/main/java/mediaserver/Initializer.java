package mediaserver;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

import java.util.function.Supplier;

class Initializer extends ChannelInitializer<SocketChannel> {

    private final Supplier<Router> mediaServerRouter;

    Initializer(Supplier<Router> mediaServerRouter) {
        this.mediaServerRouter = mediaServerRouter;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new CustomHttpServerCodec());
        pipeline.addLast(mediaServerRouter.get());
    }
}
