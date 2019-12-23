package mediaserver.gui;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import mediaserver.http.Handling;
import mediaserver.http.NettyHandler;
import mediaserver.http.Prefix;
import mediaserver.http.WebPath;
import mediaserver.sessions.Sessions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FbUnauth extends NettyHandler {

    private static final Logger log = LoggerFactory.getLogger(FbUnauth.class);

    private final Sessions sessions;

    public FbUnauth(Sessions sessions) {

        super(Prefix.UNAUTH);
        this.sessions = sessions;
    }

    @Override
    public Handling handleRequest(FullHttpRequest req, WebPath webPath, ChannelHandlerContext ctx) {

        sessions.close(req).ifPresentOrElse(
            session ->
                log.info("Session logged out: {}", session),
            () ->
                log.info("No session to log out"));
        return respond(ctx, unauthCookieResponse(req, unauthCookie()));
    }

}
