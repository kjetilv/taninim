package mediaserver.gui;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import mediaserver.sessions.Sessions;
import mediaserver.util.IO;

public class FbUnauth extends Nettish {

    private final Sessions sessions;

    public FbUnauth(IO io, Sessions sessions) {

        super(io, "/unauth");

        this.sessions = sessions;
    }

    @Override
    public HttpResponse handle(FullHttpRequest req, String path, ChannelHandlerContext ctx) {

        return sessions.closeSession(req)
            .map(closed ->
                logout(path, ctx))
            .orElseGet(() ->
                super.handle(req, path, ctx));
    }

    private static HttpResponse logout(String path, ChannelHandlerContext ctx) {

        return respond(ctx, path, logoutCoookieResponse());
    }

    private static HttpResponse logoutCoookieResponse() {

        return cookieResponse(cookie(null));
    }
}
