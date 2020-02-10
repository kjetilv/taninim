package mediaserver.gui;

import mediaserver.http.*;
import mediaserver.sessions.Sessions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FbUnauth extends NettyHandler {

    private static final Logger log = LoggerFactory.getLogger(FbUnauth.class);

    private final Sessions sessions;

    public FbUnauth(Sessions sessions) {

        super(Route.UNAUTH);
        this.sessions = sessions;
    }

    @Override
    protected Handling handle(Req req) {

        sessions.close(req).ifPresentOrElse(
            session ->
                log.info("Session closed: {}", session),
            () ->
                log.info("No session to close"));
        return handle(req, Netty.unauthCookieResponse(Netty.unauthCookie()));
    }
}
