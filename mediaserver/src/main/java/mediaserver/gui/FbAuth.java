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
import mediaserver.http.Nettish;
import mediaserver.sessions.Ids;
import mediaserver.sessions.Session;
import mediaserver.sessions.Sessions;
import mediaserver.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Supplier;

public class FbAuth extends Nettish {

    private static final Logger log = LoggerFactory.getLogger(FbAuth.class);

    private final Sessions sessions;

    private final Supplier<char[]> appSecret;

    private final Supplier<Ids> ids;

    public FbAuth(
        Sessions sessions,
        Supplier<char[]> appSecret,
        Supplier<Ids> ids
    ) {

        super("/auth");
        this.sessions = sessions;
        this.appSecret = appSecret;
        this.ids = ids;
    }

    @Override
    public Optional<HttpResponse> handle(FullHttpRequest req, String path, ChannelHandlerContext ctx) {

        return sessions.activeUser(req)
            .map(user ->
                respond(ctx, HttpResponseStatus.OK))
            .or(() ->
                lookupFacebookUser(req)
                    .map(user ->
                        login(req, ctx, user)));
    }

    private HttpResponse login(HttpRequest req, ChannelHandlerContext ctx, FacebookUser facebookUser) {

        return ids().isAuthorized(facebookUser)
            ? authorizedSession(req, ctx, facebookUser)
            : unprocessed(ctx, facebookUser);
    }

    private Ids ids() {

        return ids.get();
    }

    private HttpResponse authorizedSession(
        HttpRequest req,
        ChannelHandlerContext ctx,
        FacebookUser authorizedUser
    ) {

        Session session = sessions.sessionUp(authorizedUser);
        HttpResponse response = authCookieResponse(req, session, newAuthCookie(session));
        return respond(ctx, response);
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

    private static HttpResponse unprocessed(ChannelHandlerContext ctx, FacebookUser facebookUser) {

        log.warn("Unknown user attempted login: {}/{}", facebookUser.getName(), facebookUser.getId());
        return respond(ctx, HttpResponseStatus.UNPROCESSABLE_ENTITY);
    }
}
