package mediaserver.gui;

import mediaserver.http.*;

public abstract class TemplateEnabled extends NettyHandler {

    public static final String PLAYLIST_M3U = "res/playlist.m3u";

    private final Templater templater;

    protected static final String LOGIN_PAGE = "html/login.html";

    protected static final String INDEX_PAGE = "html/index.html";

    protected static final String ALBUM_PAGE = "html/album.html";

    protected static final String DEBUG_PAGE = "html/debug.html";

    protected static final String ADMIN_PAGE = "html/admin.html";

    private static final String TEXT_HTML = "text/html";

    protected TemplateEnabled(Templater templater, Page... prefix) {

        super(prefix);
        this.templater = templater;
    }

    protected Template getTemplate(String albumPage) {

        return templater.template(albumPage);
    }

    protected Handling respondHtml(WebPath webPath, Template template) {

        return respond(webPath, Netty.response(webPath, TEXT_HTML, template.bytes()));
    }
}
