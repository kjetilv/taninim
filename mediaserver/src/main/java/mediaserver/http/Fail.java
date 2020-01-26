package mediaserver.http;

public final class Fail extends NettyHandler {

    @Override
    protected Handling handleRequest(Req req) {

        return handleBadRequest(req);
    }
}
