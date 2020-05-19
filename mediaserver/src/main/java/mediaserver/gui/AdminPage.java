package mediaserver.gui;

import mediaserver.GlobalState;
import mediaserver.externals.ACL;
import mediaserver.externals.S3Client;
import mediaserver.http.*;
import mediaserver.http.Route.Method;
import mediaserver.media.Media;
import mediaserver.sessions.AccessLevel;
import mediaserver.sessions.Ids;
import mediaserver.sessions.Session;
import mediaserver.sessions.Sessions;
import mediaserver.templates.TPar;
import mediaserver.toolkit.Templater;
import mediaserver.util.IO;
import mediaserver.util.OnceEvery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static mediaserver.http.FPar.*;
import static mediaserver.templates.TPar.user;

public final class AdminPage extends TemplateEnabled {

    private static final Logger log = LoggerFactory.getLogger(AdminPage.class);

    private final Supplier<Media> mediaSupplier;

    private final Supplier<Ids> idsSupplier;

    private final Sessions sessions;

    private final S3Client s3;

    private final Map<Action, Function<Req, Optional<Handling>>> actions = Map.of(
        Action.ids, this::updateIds,
        Action.exterminate, this::closeSession,
        Action.juke, this::juke,
        Action.nuke, this::nuke,
        Action.reload, this::reload);

    private static final Predicate<Req> IS_FORM = r ->
        APPLICATION_X_WWW_FORM_URLENCODED.contentEquals(r.header("Content-Type"));

    private static final Predicate<Req> IS_POST = r ->
        POST.equals(r.getRequest().method());

    public AdminPage(
        Route route,
        Supplier<Media> mediaSupplier,
        Supplier<Ids> idsSupplier,
        Sessions sessions,
        Templater templater,
        S3Client s3
    ) {

        super(route, templater);
        this.mediaSupplier = mediaSupplier;
        this.idsSupplier = idsSupplier;
        this.sessions = sessions;
        this.s3 = s3;
    }

    @Override
    protected Handling handle(Req req) {

        return action(req)
            .map(actions::get)
            .flatMap(action ->
                action.apply(req))
            .orElseGet(() ->
                respondHtml(req, getTemplate(ADMIN_PAGE)
                    .add(user, req.getSession().getActiveUser(req))
                    .add(TPar.sessions, sessions.list())
                    .add(TPar.ids, map(idsSupplier.get()))));
    }

    private Optional<Handling> reload(Req req) {

        OnceEvery.actuallyJustRefresh(AdminPage.this.idsSupplier, AdminPage.this.mediaSupplier);
        return Optional.of(redirect(req, true));
    }

    private Optional<Handling> nuke(Req req) {

        if (GlobalState.get().unsetGlobalTrack()) {
            log.info("Reset global jukebox track");
        } else {
            log.warn("Was asked to reset global jukebox track, was not set");
        }
        return Optional.of(redirect(req, true));
    }

    private Optional<Handling> juke(Req req) {

        GlobalState globalState = GlobalState.get();

        Media media = AdminPage.this.mediaSupplier.get();
        jukeboxTrack.id(req).flatMap(media::getTrack).findFirst().ifPresentOrElse(
            track ->
                jukeboxAlbum.id(req)
                    .flatMap(media::getAlbum)
                    .findFirst()
                    .ifPresentOrElse(
                        album -> {
                            if (jukeboxClear.isTrue(req)) {
                                globalState.unsetGlobalTrack();
                                log.info("Global jukebox track cleared");
                            } else {
                                log.info("Global jukebox track: {} / {}", album, track);
                                globalState.setGlobalTrack(req.getTime(), album, track);
                            }
                        },
                        () ->
                            log.warn("Album not found: {}", jukeboxAlbum.id(req))),
            () ->
                log.warn("Track not found: {}", jukeboxTrack.id(req)));
        return Optional.of(redirect(req, true));
    }

    private Optional<Handling> closeSession(Req req) {

        closedSession(req)
            .map(closed -> redirect(req, false))
            .orElseGet(() -> handleNotFound(req));
        return Optional.empty();
    }

    private Optional<Handling> updateIds(Req req) {

        if (AdminPage.this.s3 != null) {
            try {
                FPar.ids.get(req)
                    .map(this::readIds)
                    .forEach(Ids::persist);
            } finally {
                OnceEvery.actuallyJustRefresh(AdminPage.this.idsSupplier);
            }
        } else {
            log.warn("Could not update, no S3 connection: {}", req);
        }
        return Optional.of(redirect(req, false));
    }

    private Ids readIds(String ids) {

        try {
            return new Ids(IO.OM.readerFor(ACL.class).readValue(ids), s3);
        } catch (Exception e) {
            throw new IllegalStateException("No map", e);
        }
    }

    private Handling redirect(Req req, boolean referer) {

        return handle(req, referer
            ? Netty.redirect(req.getReferer())
            : Netty.redirect(
                new Route("admin", AccessLevel.ADMIN, Method.GET, Method.POST)));
    }

    private Optional<Session> closedSession(Req req) {

        return FPar.session.id(req)
            .flatMap(session ->
                sessions.close(session).stream())
            .peek(closed ->
                log.info("Closed session: {}", closed))
            .findFirst();
    }

    private static Optional<Action> action(Req req) {

        return Optional.ofNullable(req)
            .filter(IS_POST.and(IS_FORM))
            .flatMap(r ->
                Arrays.stream(Action.values()).filter(action -> r.getPath().startsWith(action.path())).findFirst());
    }

    private static String map(Ids ids) {

        try {
            return IO.OMP.writeValueAsString(ids.getACL());
        } catch (Exception e) {
            throw new IllegalStateException("No map", e);
        }
    }

    enum Action {

        ids, exterminate, juke, nuke, reload;

        private String path() {

            return '/' + name();
        }
    }
}
