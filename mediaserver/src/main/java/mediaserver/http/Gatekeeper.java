package mediaserver.http;

import io.netty.channel.ChannelHandlerContext;
import mediaserver.gui.TemplateEnabled;
import mediaserver.gui.Templater;
import mediaserver.sessions.Sessions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Gatekeeper extends TemplateEnabled {

    private static final Logger log = LoggerFactory.getLogger(Gatekeeper.class);

    private final Sessions sessions;

    public Gatekeeper(Sessions sessions, Templater templater) {

        super(templater);
        this.sessions = sessions;
    }

    @Override
    public Handling handleRequest(
        WebPath webPath,
        ChannelHandlerContext ctx
    ) {

        if (webPath.hasPrefix(LOGIN)) {
            return pass();
        }

        if (webPath.requiresAuthentication()) {
            return sessions.activeSession(webPath)
                .map(user -> pass())
                .orElseGet(() -> {
                    log.info("Redirecting {} to login", webPath);
                    return handle(ctx, Netty.redirectResponse(LOGIN.getPref()));
                });
        }

        return pass();
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + sessions + "]";
    }
}
