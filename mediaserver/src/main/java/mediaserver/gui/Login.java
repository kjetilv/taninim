package mediaserver.gui;

import io.netty.channel.ChannelHandlerContext;
import mediaserver.http.Handling;
import mediaserver.http.Prefix;
import mediaserver.http.WebPath;

public final class Login extends TemplateEnabled {

    public Login(Templater templater) {

        super(templater, Prefix.LOGIN);
    }

    @Override
    public Handling handleRequest(WebPath webPath, ChannelHandlerContext ctx) {

        return respond(webPath, ctx, login());
    }
}
