package mediaserver;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;

class CustomHttpRequestDecoder extends HttpObjectDecoder {

    /**
     * Creates a new instance with the specified parameters.
     */
    CustomHttpRequestDecoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize) {
        super(maxInitialLineLength, maxHeaderSize, maxChunkSize, false);
    }

    CustomHttpRequestDecoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize,
                             boolean validateHeaders) {
        super(maxInitialLineLength, maxHeaderSize, maxChunkSize, false, validateHeaders);
    }

    @Override
    protected boolean isDecodingRequest() {
        return true;
    }

    @Override
    protected HttpMessage createMessage(String[] initialLine) throws Exception {
        return new DefaultHttpRequest(
            HttpVersion.valueOf(initialLine[2]), HttpMethod.valueOf(initialLine[0]
        ),
            initialLine[1], validateHeaders);
    }

    @Override
    protected HttpMessage createInvalidMessage() {
        return new DefaultFullHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.GET, "/bad-request", Unpooled.EMPTY_BUFFER,
            validateHeaders);
    }
}
