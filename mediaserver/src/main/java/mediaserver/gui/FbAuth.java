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
        IO io,
        Sessions sessions,
        Supplier<char[]> appSecret,
        Supplier<Ids> ids
    ) {

        super(io, "/auth");
        this.sessions = sessions;
        this.appSecret = appSecret;
        this.ids = ids;
    }

    @Override
    public HttpResponse handle(FullHttpRequest req, String path, ChannelHandlerContext ctx) {

        return sessions.activeUser(req)
            .map(user ->
                respond(ctx, path, HttpResponseStatus.OK))
            .orElseGet(() ->
                facebookUser(req)
                    .map(facebookUser ->
                        login(req, path, ctx, facebookUser))
                    .orElseGet(() ->
                        super.handle(req, path, ctx)));
    }

    private HttpResponse login(HttpRequest req, String path, ChannelHandlerContext ctx, FacebookUser facebookUser) {

        return ids.get().ids().stream()
            .filter(id ->
                authorized(id, facebookUser))
            .findFirst()
            .map(authorized ->
                resolveAuthorizedSession(req, path, ctx, facebookUser))
            .orElseGet(() ->
                unprocessed(path, ctx, facebookUser));
    }

    private boolean authorized(Object entry, FacebookUser facebookUser) {

        return String.valueOf(entry).equalsIgnoreCase(facebookUser.getId());
    }

    private HttpResponse resolveAuthorizedSession(
        HttpRequest req,
        String path,
        ChannelHandlerContext ctx,
        FacebookUser facebookUser
    ) {

        Session session = sessions.sessionUp(facebookUser);
        HttpResponse response = helloCookieResponse(req, session, cookie(session));
        return respond(ctx, path, response);
    }

    private Optional<FacebookUser> facebookUser(FullHttpRequest req) {

        return Optional.of(req.content())
            .map(content ->
                content.toString(StandardCharsets.UTF_8))
            .map(json -> {
                String input = req.content().toString(StandardCharsets.UTF_8);
                FacebookAuthResponse authResponse = IO.readJson(FacebookAuthResponse.class, input);
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

    private static HttpResponse unprocessed(String path, ChannelHandlerContext ctx, FacebookUser facebookUser) {

        log.warn("Unknown user attempted login: {}/{}", facebookUser.getName(), facebookUser.getId());
        return respond(ctx, path, HttpResponseStatus.UNPROCESSABLE_ENTITY);
    }
}
