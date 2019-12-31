package mediaserver.gui;

import com.restfb.*;
import com.restfb.types.User;
import io.netty.channel.ChannelHandlerContext;
import mediaserver.externals.FacebookAuthResponse;
import mediaserver.externals.FacebookUser;
import mediaserver.http.*;
import mediaserver.sessions.AccessLevel;
import mediaserver.sessions.Ids;
import mediaserver.sessions.Session;
import mediaserver.sessions.Sessions;
import mediaserver.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public Handling handleRequest(WebPath webPath, ChannelHandlerContext ctx) {

        return sessions.activeSession(webPath, AccessLevel.LOGIN)
            .map(session ->
                handleExisting(ctx, session))
            .or(() ->
                loginFacebookUser(webPath).flatMap(user ->
                    handleNewSession(webPath, user, ctx)))
            .orElseGet(() ->
                handleBadRequest(ctx));
    }

    private Handling handleExisting(ChannelHandlerContext ctx, Session session) {

        log.info("Redundant login, session already exists: {}", session);
        return handleOK(ctx);
    }

    private Optional<Handling> handleNewSession(WebPath webPath, FacebookUser user, ChannelHandlerContext ctx) {

        AccessLevel accessLevel = ids.get().getLevel(user);
        if (accessLevel == AccessLevel.NONE) {
            log.warn("Unknown user attempted login: {}/{}", user.getName(), user.getId());
            return Optional.empty();
        }
        Session session = sessions.establish(webPath, user, accessLevel);
        log.info("Recognized user logged in: {}", session);
        return Optional.of(handle(ctx, Netty.authCookieResponse(webPath, Netty.authCookie(session))));
    }

    private Optional<FacebookUser> loginFacebookUser(WebPath webPath) {

        return webPath.getContent().map(json -> {
            FacebookAuthResponse authResponse = IO.readObject(FacebookAuthResponse.class, json);
            FacebookUser facebookUser = login(authResponse);
            log.info("Logged in: {}", facebookUser);
            return facebookUser;
        });
    }

    private FacebookUser login(FacebookAuthResponse authResponse) {

        try {
            User user = facebookClient(authResponse).fetchObject(authResponse.getUserID(), User.class);
            return new FacebookUser(user.getName(), user.getId());
        } catch (Exception e) {
            throw new IllegalStateException("Login failed for user: " + authResponse.getUserID(), e);
        }
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
