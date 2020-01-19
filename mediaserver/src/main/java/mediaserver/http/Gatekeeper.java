package mediaserver.http;

import mediaserver.gui.TemplateEnabled;
import mediaserver.sessions.AccessLevel;
import mediaserver.toolkit.Templater;
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

        if (webPath.isAllowed()) {
            return pass();
        }
        if (canLogin(webPath)) {
            return redirectToLogin(webPath);
        }

        return handleBadRequest(webPath);
    }

    private boolean canLogin(WebPath webPath) {

        return webPath.getPage().accessibleWith(AccessLevel.LOGIN);
    }

    private Handling redirectToLogin(WebPath webPath) {

        log.info("Redirecting {} to {}", webPath, Page.LOGIN.getPref());
        return respond(webPath, Netty.redirectResponse(Page.LOGIN));
    }
}
