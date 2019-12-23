package mediaserver.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import mediaserver.gui.TemplateEnabled;
import mediaserver.gui.Templater;
import mediaserver.sessions.Sessions;

import static mediaserver.http.Handling.pass;

public final class Gatekeeper extends TemplateEnabled {

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
                .map(user ->
                    pass())
                .orElseGet(() ->
                    redirect(ctx, LOGIN.getPref()));
        }
        return pass();
    }
}
