package mediaserver.gui;

import com.restfb.*;
import com.restfb.types.User;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import mediaserver.externals.FacebookAuthResponse;
import mediaserver.externals.FacebookUser;
import mediaserver.http.Handling;
import mediaserver.http.NettyHandler;
import mediaserver.http.Prefix;
import mediaserver.http.WebPath;
import mediaserver.sessions.Ids;
import mediaserver.sessions.Session;
import mediaserver.sessions.Sessions;
import mediaserver.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Supplier;

public final class FbAuth extends NettyHandler {

    private static final Logger log = LoggerFactory.getLogger(FbAuth.class);

    private final Sessions sessions;

    private final Supplier<char[]> appSecret;

    private final Supplier<Ids> ids;

    public FbAuth(Sessions sessions, Supplier<Ids> ids, Supplier<char[]> appSecret) {

        super(Prefix.AUTH);
        this.sessions = sessions;
        this.appSecret = appSecret;
        this.ids = ids;
    }

    @Override
    public Handling handleRequest(FullHttpRequest req, WebPath webPath, ChannelHandlerContext ctx) {

        return sessions.activeUser(req)
            .map(user ->
                respond(ctx, HttpResponseStatus.OK))
            .orElseGet(() ->
                lookupFacebookUser(req)
                    .map(user ->
                        login(req, ctx, user))
                    .orElseGet(Handling::pass));
    }

    private Handling login(HttpRequest req, ChannelHandlerContext ctx, FacebookUser facebookUser) {

        if (ids.get().isAuthorized(facebookUser)) {

            Session session = sessions.establish(facebookUser);
            HttpResponse response = authCookieResponse(req, session, authCookie(session));
            return respond(ctx, response);
        }

        log.warn("Unknown user attempted login: {}/{}", facebookUser.getName(), facebookUser.getId());
        return respond(ctx, HttpResponseStatus.UNPROCESSABLE_ENTITY);
    }

    private Optional<FacebookUser> lookupFacebookUser(FullHttpRequest req) {

        return Optional.of(req.content())
            .map(content ->
                content.toString(StandardCharsets.UTF_8))
            .map(json -> {
                String input = req.content().toString(StandardCharsets.UTF_8);
                FacebookAuthResponse authResponse = IO.readObject(FacebookAuthResponse.class, input);
                FacebookClient fc = facebookClient(authResponse);
                User user = fc.fetchObject(authResponse.getUserID(), User.class);
                return new FacebookUser(user.getName(), user.getId());
            });
    }

    private FacebookClient facebookClient(FacebookAuthResponse authResponse) {

        return new DefaultFacebookClient(
            authResponse.getAccessToken(),
            new String(appSecret.get()),
            new DefaultWebRequestor(),
            new DefaultJsonMapper(),
            Version.LATEST);
    }
}
