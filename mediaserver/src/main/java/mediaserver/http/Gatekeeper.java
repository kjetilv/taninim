package mediaserver.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import mediaserver.gui.TemplateEnabled;
import mediaserver.gui.Templater;
import mediaserver.sessions.Sessions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static mediaserver.http.Handling.pass;

public final class Gatekeeper extends TemplateEnabled {

    private static final Logger log = LoggerFactory.getLogger(Gatekeeper.class);

    private final Sessions sessions;

    public Gatekeeper(Sessions sessions, Templater templater) {

        super(templater);
        this.sessions = sessions;
    }

    @Override
    public Handling handleRequest(
        FullHttpRequest req,
        WebPath webPath,
        ChannelHandlerContext ctx
    ) {

        if (webPath.hasPrefix(LOGIN)) {
            return pass();
        }

        if (webPath.isAuthenticated()) {
            return sessions.activeUser(req)
                .map(user -> pass())
                .orElseGet(() -> redirect(webPath, ctx));
        }

        return pass();
    }

    private Handling redirect(WebPath webPath, ChannelHandlerContext ctx) {

        log.info("Redirecting {} to login", webPath);
        return respond(ctx, redirectResponse(LOGIN.getPref()));
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + sessions + "]";
    }
}
