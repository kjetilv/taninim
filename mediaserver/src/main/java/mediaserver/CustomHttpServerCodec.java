package mediaserver;


import io.netty.channel.CombinedChannelDuplexHandler;
import io.netty.handler.codec.http.HttpResponseEncoder;

final class CustomHttpServerCodec
    extends CombinedChannelDuplexHandler<CustomHttpRequestDecoder, HttpResponseEncoder> {

    CustomHttpServerCodec() {
        super(
            new CustomHttpRequestDecoder(
                4096,
                8192,
                8192),
            new HttpResponseEncoder());
    }
}
