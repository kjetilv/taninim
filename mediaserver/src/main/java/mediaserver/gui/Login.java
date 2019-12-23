package mediaserver.gui;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import mediaserver.http.Handling;
import mediaserver.http.Prefix;
import mediaserver.http.WebPath;

public final class Login extends TemplateEnabled {

    public Login(Templater templater) {

        super(templater, Prefix.LOGIN);
    }

    @Override
    public Handling handleRequest(FullHttpRequest req, WebPath webPath, ChannelHandlerContext ctx) {

        return respond(req, ctx, login());
    }
}
