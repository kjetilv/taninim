package mediaserver.http;

import mediaserver.gui.TemplateEnabled;
import mediaserver.gui.Templater;
import mediaserver.sessions.AccessLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Gatekeeper extends TemplateEnabled {

    private static final Logger log = LoggerFactory.getLogger(Gatekeeper.class);

    public Gatekeeper(Templater templater) {

        super(templater);
    }

    @Override
    public Handling handleRequest(
        WebPath webPath
    ) {

        if (webPath.getPage().accessibleIn(webPath.getSession())) {
            return pass();
        }
        if (webPath.getPage().accessibleWith(AccessLevel.LOGIN)) {
            return redirectToLogin(webPath);
        }
        return handleUnauthorized(webPath);
    }

    private Handling redirectToLogin(WebPath webPath) {

        log.info("Redirecting {} to {}", webPath, Page.LOGIN.getPref());
        return respond(webPath, Netty.redirectResponse(Page.LOGIN));
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[]";
    }
}
