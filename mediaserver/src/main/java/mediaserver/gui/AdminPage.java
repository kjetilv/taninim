package mediaserver.gui;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import mediaserver.GlobalState;
import mediaserver.externals.ACL;
import mediaserver.externals.S3Client;
import mediaserver.http.FPar;
import mediaserver.http.Handling;
import mediaserver.http.Netty;
import mediaserver.http.Req;
import mediaserver.http.Route;
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

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static mediaserver.http.FPar.jukeboxAlbum;
import static mediaserver.http.FPar.jukeboxClear;
import static mediaserver.http.FPar.jukeboxTrack;
import static mediaserver.templates.TPar.user;

public final class AdminPage
    extends TemplateEnabled {

    private static final Logger log = LoggerFactory.getLogger(AdminPage.class);

    enum Action {
        IDS,
        EXTERMINATE,
        JUKE,
        NUKE,
        RELOAD;

        private String path() {
            return '/' + name().toLowerCase();
        }
    }

    private final Supplier<Media> mediaSupplier;

    private final Supplier<Ids> idsSupplier;

    private final Sessions sessions;

    private final S3Client s3;

    private final Map<Action, Function<Req, Optional<Handling>>> actions = Map.of(
        Action.IDS, this::updateIds,
        Action.EXTERMINATE, this::closeSession,
        Action.JUKE, this::juke,
        Action.NUKE, this::nuke,
        Action.RELOAD, this::reload);

    public AdminPage(
        Route route,
        Supplier<Media> mediaSupplier,
        Supplier<Ids> idsSupplier,
        Sessions sessions,
        Templater templater,
        S3Client s3
    ) {
        super(route, templater);
        this.mediaSupplier = Objects.requireNonNull(mediaSupplier, "mediaSupplier");
        this.idsSupplier = Objects.requireNonNull(idsSupplier, "idsSupplier");
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.s3 = s3;
    }

    @Override

    protected Handling handle(Req req) {
        return action(req).map(actions::get)
            .flatMap(action -> action.apply(req))
            .orElseGet(() ->
                respondHtml(
                    req,
                    getTemplate(ADMIN_PAGE).add(user, req.getSession().getActiveUser(req))
                        .add(TPar.sessions, sessions.list())
                        .add(TPar.ids, map(idsSupplier.get()))));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
            "[mediaSupplier=" + mediaSupplier +
            " idsSupplier=" + idsSupplier +
            " sessions=" + sessions +
            " s3=" + s3 +
            " actions=" + actions +
            "]";
    }

    private Optional<Handling> reload(Req req) {
        OnceEvery.actuallyJustRefresh(this.idsSupplier, this.mediaSupplier);
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
        Media media = this.mediaSupplier.get();
        jukeboxTrack.id(req)
            .flatMap(media::getTrack)
            .findFirst()
            .ifPresentOrElse(
                track -> jukeboxAlbum.id(req)
                    .flatMap(media::getAlbumContext)
                    .findFirst()
                    .ifPresentOrElse(albumContext -> {
                        if (jukeboxClear.isTrue(req)) {
                            globalState.unsetGlobalTrack();
                            log.info("Global jukebox track cleared");
                        } else {
                            log.info("Global jukebox track: {} / {}", albumContext, track);
                            globalState.setGlobalTrack(req.getTime(), albumContext, track);
                        }
                    }, () -> log.warn("Album not found: {}", jukeboxAlbum.id(req))),
                () -> log.warn("Track not found: {}", jukeboxTrack.id(req)));
        return Optional.of(redirect(req, true));
    }

    private Optional<Handling> closeSession(Req req) {
        closedSession(req).map(closed -> redirect(req, false)).orElseGet(() -> handleNotFound(req));
        return Optional.empty();
    }

    private Optional<Handling> updateIds(Req req) {
        if (this.s3 != null) {
            try {
                FPar.ids.get(req).map(this::readIds).forEach(Ids::persist);
            } finally {
                OnceEvery.actuallyJustRefresh(this.idsSupplier);
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
        return handle(
            req,
            referer
                ? Netty.redirect(req.getReferer())
                : Netty.redirect(new Route("admin", AccessLevel.ADMIN, Method.GET, Method.POST)));
    }

    private Optional<Session> closedSession(Req req) {
        return FPar.session.id(req)
            .flatMap(session -> sessions.close(session).stream())
            .peek(closed -> log.info("Closed session: {}", closed))
            .findFirst();
    }

    private static final Predicate<Req>
        IS_FORM =
        r -> APPLICATION_X_WWW_FORM_URLENCODED.contentEquals(r.header("Content-Type"));

    private static final Predicate<Req> IS_POST = r -> POST.equals(r.getRequest().method());

    private static Optional<Action> action(Req req) {
        return Optional.ofNullable(req)
            .filter(IS_POST.and(IS_FORM))
            .flatMap(r -> Arrays.stream(Action.values())
                .filter(action -> r.getPath().startsWith(action.path()))
                .findFirst());
    }

    private static String map(Ids ids) {
        try {
            return IO.OMP.writeValueAsString(ids.getACL());
        } catch (Exception e) {
            throw new IllegalStateException("No map", e);
        }
    }
}
