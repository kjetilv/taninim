package mediaserver.gui;

import com.restfb.*;
import com.restfb.types.User;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import mediaserver.externals.FacebookAuthResponse;
import mediaserver.externals.FacebookUser;
import mediaserver.sessions.Session;
import mediaserver.sessions.Sessions;
import mediaserver.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class FbAuth extends Nettish {

    private static final Logger log = LoggerFactory.getLogger(FbAuth.class);

    private final Sessions sessions;

    private final Supplier<char[]> appSecret;

    private final Supplier<Map<String, ?>> ids;

    public FbAuth(
        IO io,
        Sessions sessions,
        Supplier<char[]> appSecret,
        Supplier<Map<String, ?>> ids
    ) {

        super(io, "/auth");

        this.sessions = sessions;
        this.appSecret = appSecret;
        this.ids = ids;
    }

    @Override
    public HttpResponse handle(FullHttpRequest req, String path, ChannelHandlerContext ctx) {

        return sessions.activeUser(req).map(user ->
            respond(ctx, path, HttpResponseStatus.OK)
        ).orElseGet(() ->
            facebookUser(req)
                .map(facebookUser ->
                    login(path, ctx, facebookUser))
                .orElseGet(() ->
                    super.handle(req, path, ctx)));
    }

    private static HttpResponse logout(String path, ChannelHandlerContext ctx) {

        return respond(ctx, path, logoutCoookieResponse());
    }

    private HttpResponse login(String path, ChannelHandlerContext ctx, FacebookUser facebookUser) {

        return recognizedUsers()
            .filter(entry ->
                String.valueOf(entry.getValue()).equalsIgnoreCase(facebookUser.getId()))
            .findFirst()
            .map(entry ->
                resolveSession(path, ctx, facebookUser, entry))
            .orElseGet(() ->
                unprocessed(path, ctx, facebookUser));
    }

    private Stream<? extends Map.Entry<String, ?>> recognizedUsers() {

        return ids.get().entrySet().stream();
    }

    private HttpResponse resolveSession(
        String path,
        ChannelHandlerContext ctx,
        FacebookUser facebookUser,
        Map.Entry<String, ?> e
    ) {

        Session session = sessions.sessionUp(facebookUser);
        log.info("{} logged in {}! [{}/{}]", e.getKey(), session, facebookUser.getName(), facebookUser.getId());
        return respond(ctx, path, sessionCoookieResponse(session));
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

        log.warn("Unknown user logged in: {}/{}", facebookUser.getName(), facebookUser.getId());
        return respond(ctx, path, HttpResponseStatus.UNPROCESSABLE_ENTITY);
    }

    private static HttpResponse sessionCoookieResponse(Session session) {

        return cookieResponse(cookie(session));
    }

    private static HttpResponse logoutCoookieResponse() {

        return cookieResponse(cookie(null));
    }

}
