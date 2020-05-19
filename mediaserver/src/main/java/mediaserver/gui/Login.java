package mediaserver.gui;

import mediaserver.http.Handling;
import mediaserver.http.Req;
import mediaserver.http.Route;
import mediaserver.toolkit.Templater;

public final class Login extends TemplateEnabled {

    public Login(Route route, Templater templater) {

        super(route, templater);
    }

    @Override
    protected Handling handle(Req req) {

        return respondHtml(req, getTemplate(LOGIN_PAGE));
    }
}
