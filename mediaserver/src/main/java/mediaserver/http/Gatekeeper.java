package mediaserver.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import mediaserver.sessions.Sessions;

import java.util.Optional;

public class Gatekeeper extends Nettish {

    private final Sessions sessions;

    public Gatekeeper(Sessions sessions) {

        super("/");
        this.sessions = sessions;
    }

    @Override
    public Optional<HttpResponse> handle(FullHttpRequest req, String path, ChannelHandlerContext ctx) {

        return sessions.activeUser(req)
            .map(user ->
                Optional.<HttpResponse>empty())
            .orElseGet(() ->
                Optional.of(redirect(ctx, "/login")));
    }
}
