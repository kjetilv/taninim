package mediaserver.http;

public final class Fail extends NettyHandler {

    @Override
    public Handling handleRequest(WebPath webPath) {

        return handleBadRequest(webPath);
    }
}
