package mediaserver.http;

import io.netty.handler.codec.http.HttpResponse;

import java.util.Objects;

public final class Handling {

    private final HttpResponse sentResponse;

    private Handling(HttpResponse sentResponse) {

        this.sentResponse = sentResponse;
    }

    public static Handling sentResponse(HttpResponse response) {

        return new Handling(Objects.requireNonNull(response, "response"));
    }

    public static Handling pass() {

        return new Handling(null);
    }

    public HttpResponse getSentResponse() {

        return sentResponse;
    }

    public boolean isDone() {

        return sentResponse != null;
    }

    boolean isPass() {

        return sentResponse == null;
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + sentResponse.status() + "]";
    }
}
