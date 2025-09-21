package taninim.fb;

import module java.base;
import module java.net.http;
import com.restfb.DebugHeaderInfo;
import com.restfb.WebRequestor;

class SimpleRequestor implements WebRequestor {

    @Override
    public Response executeGet(Request request) throws IOException {
        HttpResponse<String> send;
        try {
            send = HTTP_CLIENT.send(getBuild(request), STRING);
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

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().build();

    private static final HttpResponse.BodyHandler<String> STRING = HttpResponse.BodyHandlers.ofString();

    private static HttpRequest getBuild(Request request) {
        return HttpRequest.newBuilder().GET().uri(URI.create(request.getFullUrl())).build();
    }

    private static <T> T fail() {
        throw new UnsupportedOperationException("Not supported");
    }
}
