package mediaserver.gui;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import mediaserver.http.Handling;
import mediaserver.http.NettyHandler;
import mediaserver.http.Prefix;

public abstract class TemplateEnabled extends NettyHandler {

    protected static final Prefix LOGIN = Prefix.LOGIN;

    protected static final Prefix ALBUM = Prefix.ALBUM;

    private final Templater templater;

    protected static final String LOGIN_PAGE = "res/login.html";

    private static final String INDEX_PAGE = "res/index.html";

    private static final String ALBUM_PAGE = "res/album.html";

    private static final String TEXT_HTML = "text/html";

    protected TemplateEnabled(Templater templater, Prefix prefix) {

        super(prefix);
        this.templater = templater;
    }

    protected Template login() {

        return templater.template(LOGIN_PAGE);
    }

    protected Template index() {

        return templater.template(INDEX_PAGE);
    }

    protected Template album() {

        return templater.template(ALBUM_PAGE);
    }

    protected Handling respond(FullHttpRequest req, ChannelHandlerContext ctx, Template t) {

        return respond(ctx, response(req, null, TEXT_HTML, t.bytes(), null));
    }
}
