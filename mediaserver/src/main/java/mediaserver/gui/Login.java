package mediaserver.gui;

import javax.annotation.Nonnull;

import mediaserver.http.Handling;
import mediaserver.http.Req;
import mediaserver.http.Route;
import mediaserver.toolkit.Templater;

public final class Login extends TemplateEnabled {

    public Login(
        @Nonnull Route route,
        @Nonnull Templater templater) {
        super(route, templater);
    }

    protected @Override @Nonnull Handling handle(Req req) {
        return respondHtml(req, getTemplate(LOGIN_PAGE));
    }
}
