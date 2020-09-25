package mediaserver.gui;

import java.util.Objects;

import mediaserver.http.Handling;
import mediaserver.http.Netty;
import mediaserver.http.NettyHandler;
import mediaserver.http.Req;
import mediaserver.http.Route;
import mediaserver.templates.Template;
import mediaserver.toolkit.Templater;

public abstract class TemplateEnabled extends NettyHandler {

    static final String PLAYLIST_M3U = "playlist.m3u.st";

    static final String LOGIN_PAGE = "html/login.html.st";

    static final String INDEX_PAGE = "html/index.html.st";

    static final String ALBUM_PAGE = "html/album.html.st";

    static final String ADMIN_PAGE = "html/admin.html.st";

    protected static final String DEBUG_PAGE = "html/debug.html.st";

    private final Templater templater;

    protected TemplateEnabled(Route route, Templater templater) {
        super(route);
        this.templater = Objects.requireNonNull(templater, "templater");
    }

    protected Template getTemplate(String albumPage) {
        return templater.template(albumPage);
    }

    protected Handling respondHtml(Req req, Template template) {
        return handle(req, Netty.response(req, TEXT_HTML, template.bytes()));
    }

    private static final String TEXT_HTML = "text/html";
}
