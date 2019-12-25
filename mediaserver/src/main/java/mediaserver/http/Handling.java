package mediaserver.http;

import io.netty.handler.codec.http.HttpResponse;

import java.util.Objects;

public final class Handling {

    private final NettyHandler handler;

    private final HttpResponse sentResponse;

    private Handling(NettyHandler handler, HttpResponse sentResponse) {

        this.handler = Objects.requireNonNull(handler, "handler");
        this.sentResponse = sentResponse;
    }

    public static Handling sentResponse(NettyHandler handler, HttpResponse response) {

        return new Handling(handler, Objects.requireNonNull(response, "response"));
    }

    public static Handling pass(NettyHandler handler) {

        return new Handling(handler, null);
    }

    public HttpResponse getSentResponse() {

        return sentResponse;
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
