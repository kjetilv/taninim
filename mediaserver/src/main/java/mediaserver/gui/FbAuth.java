package mediaserver.gui;

import com.restfb.*;
import com.restfb.types.ProfilePictureSource;
import com.restfb.types.User;
import mediaserver.externals.FacebookAuthResponse;
import mediaserver.externals.FbUser;
import mediaserver.http.*;
import mediaserver.sessions.AccessLevel;
import mediaserver.sessions.Ids;
import mediaserver.sessions.Sessions;
import mediaserver.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
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
    protected Handling handleRequest(Req req) {

        return loginFacebookUser(req).flatMap(fbUser -> {
            AccessLevel accessLevel = ids.get().resolve(fbUser);
            if (accessLevel.satisfies(AccessLevel.LOGIN)) {
                UUID cookie = sessions.newSessionUUID(req, fbUser, accessLevel);
                return Optional.of(
                    handle(req, Netty.authCookieResponse(req, Netty.authCookie(cookie))));
            }
            log.warn("Unknown user {} attempted login with access level {}", fbUser, accessLevel);
            return Optional.empty();
        }).orElseGet(() ->
            handleUnauthorized(req));
    }

    private Optional<FbUser> loginFacebookUser(Req req) {

        return req.getContent().map(json -> {
            FacebookAuthResponse authResponse = IO.readObject(FacebookAuthResponse.class, json);
            User facebookApiUser = lookup(authResponse);
            FbUser fbUser =
                new FbUser(facebookApiUser.getName(), facebookApiUser.getId());
            log.info("Facebook user logged in: {}", fbUser);
            return fbUser;
        });
    }

    private Optional<URL> picture(User facebookApiUser) {

        return Optional.ofNullable(facebookApiUser.getPicture())
            .map(ProfilePictureSource::getUrl)
            .map(URI::create)
            .flatMap(uri -> {
                try {
                    return Optional.of(uri.toURL());
                } catch (MalformedURLException e) {
                    log.warn("{} had invalid picture: {}", facebookApiUser, uri, e);
                    return Optional.empty();
                }
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
