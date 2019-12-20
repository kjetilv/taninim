package mediaserver.gui;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import mediaserver.http.Nettish;
import mediaserver.sessions.Session;
import mediaserver.sessions.Sessions;

import java.util.Optional;

public class FbUnauth extends Nettish {

    private final Sessions sessions;

    public FbUnauth(Sessions sessions) {

        super("/unauth");

        this.sessions = sessions;
    }

    @Override
    public Optional<HttpResponse> handle(FullHttpRequest req, String path, ChannelHandlerContext ctx) {

        return sessions.closeSession(req)
            .map(closed ->
                respond(ctx, logoutCoookieResponse(req, closed)));
    }

    private static HttpResponse logoutCoookieResponse(HttpRequest req, Session session) {

        return authCookieResponse(req, session, newAuthCookie(null));
    }
}
