package mediaserver;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;

class CustomHttpRequestDecoder extends HttpObjectDecoder {

    CustomHttpRequestDecoder(
        int maxInitialLineLength,
        int maxHeaderSize,
        int maxChunkSize
    ) {
        super(
            maxInitialLineLength,
            maxHeaderSize,
            maxChunkSize,
            false);
    }

    @Override
    protected boolean isDecodingRequest() {
        return true;
    }

    @Override
    protected HttpMessage createMessage(String[] initialLine) {
        return new DefaultHttpRequest(
            HttpVersion.valueOf(initialLine[2]),
            HttpMethod.valueOf(initialLine[0]),
            initialLine[1],
            validateHeaders);
    }

    @Override
    protected HttpMessage createInvalidMessage() {
        return new DefaultFullHttpRequest(
            HttpVersion.HTTP_1_0,
            HttpMethod.GET,
            "/bad-request",
            Unpooled.EMPTY_BUFFER,
            validateHeaders);
    }
}
