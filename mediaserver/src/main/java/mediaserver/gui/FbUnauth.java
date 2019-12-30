package mediaserver.gui;

import io.netty.channel.ChannelHandlerContext;
import mediaserver.http.*;
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
    public Handling handleRequest(WebPath webPath, ChannelHandlerContext ctx) {

        sessions.close(webPath).ifPresentOrElse(
            session ->
                log.info("Session closed: {}", session),
            () ->
                log.info("No session to close"));
        return sendResponse(
            ctx,
            Netty.unauthCookieResponse(Netty.unauthCookie()));
    }

}
