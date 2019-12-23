package mediaserver.gui;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import mediaserver.http.Handling;
import mediaserver.http.NettyHandler;
import mediaserver.http.Prefix;
import mediaserver.http.WebPath;
import mediaserver.sessions.Sessions;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public final class FbUnauth extends NettyHandler {

    private final Sessions sessions;

    public FbUnauth(Sessions sessions) {

        super(Prefix.UNAUTH);
        this.sessions = sessions;
    }

    @Override
    public Handling handleRequest(FullHttpRequest req, WebPath webPath, ChannelHandlerContext ctx) {

        return sessions.close(req)
            .map(closed ->
                respond(ctx, authCookieResponse(req, closed, unauthCookie())))
            .orElseGet(() ->
                respond(ctx, OK));
    }

}
