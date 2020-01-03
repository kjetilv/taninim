package mediaserver.gui;

import mediaserver.http.*;

public abstract class TemplateEnabled extends NettyHandler {

    private final Templater templater;

    protected static final String LOGIN_PAGE = "res/login.html";

    protected static final String INDEX_PAGE = "res/index.html";

    protected static final String ALBUM_PAGE = "res/album.html";

    protected static final String ADMIN_PAGE = "res/admin.html";

    private static final String TEXT_HTML = "text/html";

    public static final String PLAYLIST_M3U = "res/playlist.m3u";

    protected TemplateEnabled(Templater templater, Page... prefix) {

        super(prefix);
        this.templater = templater;
    }

    protected Template getTemplate(String albumPage) {

        return templater.template(albumPage);
    }

    protected Handling respondHtml(WebPath webPath, Template template) {

        return respondPath(webPath, Netty.response(webPath, TEXT_HTML, template.bytes()));
    }
}
