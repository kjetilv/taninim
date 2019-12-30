package mediaserver.gui;

import io.netty.channel.ChannelHandlerContext;
import mediaserver.http.*;

public abstract class TemplateEnabled extends NettyHandler {

    protected static final Prefix LOGIN = Prefix.LOGIN;

    protected static final Prefix ALBUM = Prefix.ALBUM;

    private final Templater templater;

    protected static final String LOGIN_PAGE = "res/login.html";

    private static final String INDEX_PAGE = "res/index.html";

    private static final String ALBUM_PAGE = "res/album.html";

    private static final String TEXT_HTML = "text/html";

    public static final String PLAYLIST_M3U = "res/playlist.m3u";

    protected TemplateEnabled(Templater templater, Prefix... prefix) {

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

    protected Handling respondHtml(WebPath webPath, ChannelHandlerContext ctx, Template template) {

        return sendResponse(ctx, Netty.response(webPath, TEXT_HTML, template.bytes()));
    }
}
