package mediaserver.gui;

import mediaserver.http.Handling;
import mediaserver.http.Route;
import mediaserver.http.Req;
import mediaserver.toolkit.Templater;

public final class Login extends TemplateEnabled {

    public Login(Templater templater) {

        super(templater, Route.LOGIN);
    }

    @Override
    protected Handling handleRequest(Req req) {

        return respondHtml(req, getTemplate(LOGIN_PAGE));
    }
}
