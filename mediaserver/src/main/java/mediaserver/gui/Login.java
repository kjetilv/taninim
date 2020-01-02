package mediaserver.gui;

import mediaserver.http.Handling;
import mediaserver.http.Page;
import mediaserver.http.WebPath;

public final class Login extends TemplateEnabled {

    public Login(Templater templater) {

        super(templater, Page.LOGIN);
    }

    @Override
    public Handling handleRequest(WebPath webPath) {

        return respondHtml(webPath, login());
    }
}
