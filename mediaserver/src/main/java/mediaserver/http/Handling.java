package mediaserver.http;

import io.netty.handler.codec.http.HttpResponse;

import java.util.Objects;

public final class Handling {

    private final NettyHandler handler;

    private final Req req;

    private final HttpResponse sentResponse;

    private Handling(NettyHandler handler, Req req, HttpResponse sentResponse) {

        this.handler = handler;
        this.req = req;
        this.sentResponse = sentResponse;
    }

    public static Handling sentResponse(NettyHandler handler, Req req, HttpResponse response) {

        return new Handling(handler, req, Objects.requireNonNull(response, "response"));
    }

    public HttpResponse getSentResponse() {

        return sentResponse;
    }

    public Req getReq() {

        return req;
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
