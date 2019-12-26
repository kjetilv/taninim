package mediaserver.gui;

import com.restfb.*;
import com.restfb.types.User;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponse;
import mediaserver.externals.FacebookAuthResponse;
import mediaserver.externals.FacebookUser;
import mediaserver.http.*;
import mediaserver.sessions.Ids;
import mediaserver.sessions.Session;
import mediaserver.sessions.Sessions;
import mediaserver.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

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
    public Handling handleRequest(WebPath webPath, ChannelHandlerContext ctx) {

        return sessions.activeSession(webPath)
            .map(user ->
                handle(ctx, OK))
            .orElseGet(() ->
                lookupFacebookUser(webPath)
                    .map(login(webPath, ctx))
                    .orElseGet(this::pass));
    }

    private Function<FacebookUser, Handling> login(WebPath webPath, ChannelHandlerContext ctx) {

        return user -> {
            if (ids.get().isAuthorized(user)) {
                Session session = sessions.establish(user);
                HttpResponse response =
                    Netty.authCookieResponse(webPath, Netty.authCookie(session));
                return handle(ctx, response);
            }
            log.warn("Unknown user attempted login: {}/{}", user.getName(), user.getId());
            return handle(ctx, BAD_REQUEST);
        };
    }

    private Optional<FacebookUser> lookupFacebookUser(WebPath webPath) {

        return webPath.getContent()
            .map(json -> {
                FacebookAuthResponse authResponse = IO.readObject(FacebookAuthResponse.class, json);
                User user = facebookClient(authResponse)
                    .fetchObject(authResponse.getUserID(), User.class);
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
