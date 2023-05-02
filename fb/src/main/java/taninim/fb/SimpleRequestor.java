package taninim.fb;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.restfb.DebugHeaderInfo;
import com.restfb.WebRequestor;

class SimpleRequestor implements WebRequestor {

    @Override
    public Response executeGet(Request request) throws IOException {
        HttpResponse<String> send;
        try {
            send = HttpClient.newBuilder()
                .build()
                .send(
                    HttpRequest.newBuilder().GET().uri(URI.create(request.getFullUrl())).build(),
                    HttpResponse.BodyHandlers.ofString()
                );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted", e);
        }
        return new Response(send.statusCode(), send.body());
    }

    @Override
    public Response executePost(Request request) {
        return fail();
    }

    @Override
    public Response executeDelete(Request request) {
        return fail();
    }

    @Override
    public DebugHeaderInfo getDebugHeaderInfo() {
        return null;
    }

    private static <T> T fail() {
        throw new UnsupportedOperationException("Not supported");
    }
}
