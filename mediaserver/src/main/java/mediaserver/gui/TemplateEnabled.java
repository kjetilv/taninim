package mediaserver.gui;

import mediaserver.http.*;
import mediaserver.templates.Template;
import mediaserver.toolkit.Templater;

public abstract class TemplateEnabled extends NettyHandler {

    static final String PLAYLIST_M3U = "playlist.m3u";

    private final Templater templater;

    static final String LOGIN_PAGE = "html/login.html";

    static final String INDEX_PAGE = "html/index.html";

    static final String ALBUM_PAGE = "html/album.html";

    protected static final String DEBUG_PAGE = "html/debug.html";

    static final String ADMIN_PAGE = "html/admin.html";

    private static final String TEXT_HTML = "text/html";

    protected TemplateEnabled(Templater templater, Route page) {

        super(page);
        this.templater = templater;
    }

    protected Template getTemplate(String albumPage) {

        return templater.template(albumPage);
    }

    protected Handling respondHtml(Req req, Template template) {

        return handle(req, Netty.response(req, TEXT_HTML, template.bytes()));
    }
}
