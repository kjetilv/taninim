package mediaserver.gui;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import com.restfb.DefaultFacebookClient;
import com.restfb.DefaultJsonMapper;
import com.restfb.DefaultWebRequestor;
import com.restfb.FacebookClient;
import com.restfb.Version;
import com.restfb.types.User;
import mediaserver.externals.FacebookAuthResponse;
import mediaserver.externals.FbUser;
import mediaserver.http.Handling;
import mediaserver.http.Netty;
import mediaserver.http.NettyHandler;
import mediaserver.http.Req;
import mediaserver.http.Route;
import mediaserver.sessions.AccessLevel;
import mediaserver.sessions.Ids;
import mediaserver.sessions.Session;
import mediaserver.sessions.Sessions;
import mediaserver.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FbAuth extends NettyHandler {

    private static final Logger log = LoggerFactory.getLogger(FbAuth.class);

    private final Sessions sessions;

    private final Supplier<char[]> appSecret;

    private final Supplier<Ids> ids;

    public FbAuth(Route route, Sessions sessions, Supplier<Ids> ids, Supplier<char[]> appSecret) {
        super(route);
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.appSecret = Objects.requireNonNull(appSecret, "appSecret");
        this.ids = Objects.requireNonNull(ids, "ids");
    }

    @Override

    protected Handling handle(Req req) {
        return req.getContent()
            .map(json ->
                IO.readObject(FacebookAuthResponse.class, json))
            .flatMap(this::authenticate)
            .map(user ->
                handleLogin(req, user))
            .orElseGet(() ->
                handleBadRequest(req));
    }

    private Handling handleLogin(Req req, FbUser user) {
        return ids.get().resolve(user).satisfies(AccessLevel.LOGIN)
            ? handleNewSession(req, user)
            : handleRejection(req, user);
    }

    private Handling handleRejection(Req req, FbUser user) {
        log.warn("Unknown user attempted login: {}", user);
        return handleUnauthorized(req);
    }

    private Handling handleNewSession(Req req, FbUser user) {
        Session session = sessions.create(req, user);
        log.info("{} logged in: {}", user, session);
        return handle(req, Netty.authCookieResponse(req, Netty.authCookie(session.getCookie())));
    }

    private Optional<FbUser> authenticate(FacebookAuthResponse authResponse) {
        try {
            User value = facebookClient(authResponse).fetchObject(authResponse.getUserID(), User.class);
            return Optional.of(value)
                .map(user -> new FbUser(user.getName(), user.getId()));
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
