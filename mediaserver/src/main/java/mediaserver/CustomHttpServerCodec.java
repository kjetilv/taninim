package mediaserver;


import io.netty.channel.CombinedChannelDuplexHandler;
import io.netty.handler.codec.http.HttpResponseEncoder;

final class CustomHttpServerCodec
    extends CombinedChannelDuplexHandler<CustomHttpRequestDecoder, HttpResponseEncoder> {

    /**
     * Creates a new instance with the default decoder options ({@code maxInitialLineLength (4096}},
     * {@code maxHeaderSize (8192)}, and {@code maxChunkSize (8192)}).
     */
    CustomHttpServerCodec() {
        this(4096, 8192, 8192);
    }

    /**
     * Creates a new instance with the specified decoder options.
     */
    private CustomHttpServerCodec(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize) {
        super(
            new CustomHttpRequestDecoder(
                maxInitialLineLength, maxHeaderSize, maxChunkSize),
            new HttpResponseEncoder());
    }

    /**
     * Creates a new instance with the specified decoder options.
     */
    public CustomHttpServerCodec(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize,
                                 boolean validateHeaders) {
        super(
            new CustomHttpRequestDecoder(
                maxInitialLineLength, maxHeaderSize, maxChunkSize, validateHeaders),
            new HttpResponseEncoder());
    }
}
