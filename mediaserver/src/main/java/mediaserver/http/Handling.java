package mediaserver.http;

import io.netty.handler.codec.http.HttpResponse;

import java.util.Objects;

public final class Handling {

    private final NettyHandler handler;

    private final WebPath webPath;

    private final HttpResponse sentResponse;

    private Handling(NettyHandler handler, WebPath webPath, HttpResponse sentResponse) {

        this.handler = handler;
        this.webPath = webPath;
        this.sentResponse = sentResponse;
    }

    public static Handling sentResponse(NettyHandler handler, WebPath webPath, HttpResponse response) {

        return new Handling(handler, webPath, Objects.requireNonNull(response, "response"));
    }

    public static Handling pass(NettyHandler handler) {

        return new Handling(handler, null, null);
    }

    public HttpResponse getSentResponse() {

        return sentResponse;
    }

    public WebPath getWebPath() {

        return webPath;
    }

    public NettyHandler getHandler() {

        return handler;
    }

    public boolean isHandled() {

        return sentResponse != null;
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" +
            handler + ": " + (sentResponse == null ? "PASS" : sentResponse.status()) +
            "]";
    }
}
