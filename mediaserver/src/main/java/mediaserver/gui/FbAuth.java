package mediaserver.gui;

import com.restfb.*;
import com.restfb.types.User;
import mediaserver.externals.FacebookAuthResponse;
import mediaserver.externals.FacebookUser;
import mediaserver.http.*;
import mediaserver.sessions.AccessLevel;
import mediaserver.sessions.Ids;
import mediaserver.sessions.Sessions;
import mediaserver.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class FbAuth extends NettyHandler {

    private static final Logger log = LoggerFactory.getLogger(FbAuth.class);

    private final Sessions sessions;

    private final Supplier<char[]> appSecret;

    private final Supplier<Ids> ids;

    public FbAuth(Sessions sessions, Supplier<Ids> ids, Supplier<char[]> appSecret) {

        super(Page.AUTH);
        this.sessions = sessions;
        this.appSecret = appSecret;
        this.ids = ids;
    }

    @Override
    public Handling handleRequest(WebPath webPath) {

        return loginFacebookUser(webPath).flatMap(facebookUser -> {
            AccessLevel accessLevel = ids.get().resolve(facebookUser);
            if (accessLevel.is(AccessLevel.LOGIN)) {
                UUID cookie = sessions.newSessionUUID(webPath, facebookUser, accessLevel);
                return Optional.of(
                    respondPath(webPath, Netty.authCookieResponse(webPath, Netty.authCookie(cookie))));
            }
            log.warn("Unknown user {} attempted login with access level {}", facebookUser, accessLevel);
            return Optional.empty();
        }).orElseGet(() ->
            handleBadRequest(webPath));
    }

    private Optional<FacebookUser> loginFacebookUser(WebPath webPath) {

        return webPath.getContent().map(json -> {
            FacebookAuthResponse authResponse = IO.readObject(FacebookAuthResponse.class, json);
            User facebookApiUser = lookup(authResponse);
            FacebookUser facebookUser = new FacebookUser(facebookApiUser.getName(), facebookApiUser.getId());
            log.info("Facebook user logged in: {}", facebookUser);
            return facebookUser;
        });
    }

    private User lookup(FacebookAuthResponse authResponse) {

        try {
            return facebookClient(authResponse).fetchObject(authResponse.getUserID(), User.class);
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
