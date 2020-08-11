package mediaserver.http;

import java.util.Objects;

import io.netty.handler.codec.http.HttpResponse;

public final class Handling {
    
    public static Handling sentResponse(NettyHandler handler, Req req, HttpResponse response) {
        return new Handling(handler, req, Objects.requireNonNull(response, "response"));
    }
    
    private final NettyHandler handler;
    
    private final Req req;
    
    private final HttpResponse sentResponse;
    
    private Handling(NettyHandler handler, Req req, HttpResponse sentResponse) {
        this.handler = handler;
        this.req = req;
        this.sentResponse = sentResponse;
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
