package mediaserver;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

import java.util.function.Supplier;

class MediaServerInitializer extends ChannelInitializer<SocketChannel> {

    private final Supplier<MediaServerRouter> mediaServerRouter;

    MediaServerInitializer(Supplier<MediaServerRouter> mediaServerRouter) {
        this.mediaServerRouter = mediaServerRouter;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new CustomHttpServerCodec());
        pipeline.addLast(mediaServerRouter.get());
    }
}
