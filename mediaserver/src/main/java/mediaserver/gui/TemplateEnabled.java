package mediaserver.gui;

import mediaserver.http.Handling;
import mediaserver.http.Netty;
import mediaserver.http.NettyHandler;
import mediaserver.http.Req;
import mediaserver.http.Route;
import mediaserver.templates.Template;
import mediaserver.toolkit.Templater;

public abstract class TemplateEnabled
    extends NettyHandler
{

    private final Templater templater;

    protected TemplateEnabled(Route route, Templater templater)
    {

        super(route);
        this.templater = templater;
    }

    protected Template getTemplate(String albumPage)
    {

        return templater.template(albumPage);
    }

    protected Handling respondHtml(Req req, Template template)
    {

        return handle(req, Netty.response(req, TEXT_HTML, template.bytes()));
    }

    protected static final String DEBUG_PAGE = "html/debug.html";

    static final String PLAYLIST_M3U = "playlist.m3u";

    static final String LOGIN_PAGE = "html/login.html";

    static final String INDEX_PAGE = "html/index.html";

    static final String ALBUM_PAGE = "html/album.html";

    static final String ADMIN_PAGE = "html/admin.html";

    private static final String TEXT_HTML = "text/html";
}
