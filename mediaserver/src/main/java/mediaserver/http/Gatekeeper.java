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
    public Handling handleRequest(WebPath webPath) {

        if (webPath.isAllowed()) {
            return pass();
        }

        if (webPath.getPage().accessibleWith(AccessLevel.LOGIN)) {

            log.info("Redirecting {} to {}", webPath, Page.LOGIN.getPref());
            return respond(webPath, Netty.redirectResponse(Page.LOGIN));
        }

        return handleBadRequest(webPath);
    }
}
