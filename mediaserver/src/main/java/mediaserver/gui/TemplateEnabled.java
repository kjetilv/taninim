package mediaserver.gui;

import mediaserver.http.*;

public abstract class TemplateEnabled extends NettyHandler {

    private final Templater templater;

    protected static final String LOGIN_PAGE = "res/login.html";

    private static final String INDEX_PAGE = "res/index.html";

    private static final String ALBUM_PAGE = "res/album.html";

    private static final String ADMIN_PAGE = "res/admin.html";

    private static final String TEXT_HTML = "text/html";

    public static final String PLAYLIST_M3U = "res/playlist.m3u";

    protected TemplateEnabled(Templater templater, Page... prefix) {

        super(prefix);
        this.templater = templater;
    }

    protected Template playlists() {

        return templater.template(PLAYLIST_M3U);
    }

    protected Template login() {

        return templater.template(LOGIN_PAGE);
    }

    protected Template indexTemplate() {

        return templater.template(INDEX_PAGE);
    }

    protected Template albumTemplate() {

        return templater.template(ALBUM_PAGE);
    }

    protected Template adminTemplate() {

        return templater.template(ADMIN_PAGE);
    }

    protected Handling respondHtml(WebPath webPath, Template template) {

        return respondPath(webPath, Netty.response(webPath, TEXT_HTML, template.bytes()));
    }
}
