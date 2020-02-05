package mediaserver.gui;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.MixedAttribute;
import mediaserver.Globals;
import mediaserver.externals.ACL;
import mediaserver.externals.S3;
import mediaserver.http.*;
import mediaserver.media.Media;
import mediaserver.sessions.Ids;
import mediaserver.sessions.Session;
import mediaserver.sessions.Sessions;
import mediaserver.templates.TPar;
import mediaserver.toolkit.Templater;
import mediaserver.util.IO;
import mediaserver.util.OnceEvery;
import mediaserver.util.P2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static mediaserver.http.FPar.*;

@SuppressWarnings("SameParameterValue")
public final class AdminPage extends TemplateEnabled {

    public static final String FORM_URLENCODED = "application/x-www-form-urlencoded";

    private static final Logger log = LoggerFactory.getLogger(AdminPage.class);

    private static final String ACTION_IDS = "ids";

    private static final String ACTION_EXTERMINATE = "exterminate";

    private static final String ACTION_JUKEBOX = "jukebox";

    private static final String ACTION_EXJUKE = "exjuke";

    private final Supplier<Media> media;

    private final Supplier<Ids> ids;

    private final Sessions sessions;

    private final S3.Client s3;

    public AdminPage(Supplier<Media> media, Supplier<Ids> ids, Sessions sessions, Templater templater, S3.Client s3) {

        super(templater, Page.ADMIN);
        this.media = media;
        this.ids = ids;
        this.sessions = sessions;
        this.s3 = s3;
    }

    @Override
    protected Handling handleRequest(Req req) {

        if (is(req, ACTION_IDS)) {
            if (s3 != null) {
                try {
                    params(req, IDS)
                        .map(this::readIds)
                        .forEach(Ids::persist);
                } finally {
                    OnceEvery.refresh(ids);
                }
            } else {
                log.warn("Could not update, no S3 connection: {}", req);
            }
            return redirect(req, false);
        }

        if (is(req, ACTION_EXTERMINATE)) {
            return closedSession(req)
                .map(closed ->
                    redirect(req, false))
                .orElseGet(() ->
                    handleNotFound(req));
        }

        if (is(req, ACTION_EXJUKE)) {
            Globals.get().unsetGlobalTrack();
            return redirect(req, true);
        }

        if (is(req, ACTION_JUKEBOX)) {
            Media media = this.media.get();
            uuid(req, JUKEBOX_TRACK).flatMap(media::getTrack).ifPresentOrElse(
                track ->
                    uuid(req, JUKEBOX_ALBUM).flatMap(media::getAlbum).ifPresentOrElse(
                        album -> {
                            log.info("Global jukebox track: {} / {}", album, track);
                            if (isTrue(req, JUKEBOX_CLEAR)) {
                                Globals.get().unsetGlobalTrack();
                            } else {
                                Globals.get().setGlobalTrack(req.getTime(), new P2<>(album, track));
                            }
                        },
                        () ->
                            log.warn("Album not found: {}", uuid(req, JUKEBOX_ALBUM))),
                () ->
                    log.warn("Track not found: {}", uuid(req, JUKEBOX_TRACK)));
            return redirect(req, true);
        }

        return Optional.of(getTemplate(ADMIN_PAGE)
            .add(TPar.USER, req.getSession().getActiveUser(req))
            .add(TPar.SESSIONS, sessions.list())
            .add(TPar.IDS, map(ids.get())))
            .map(template ->
                respondHtml(req, template))
            .orElseGet(() ->
                handleNotFound(req));
    }

    private Optional<UUID> uuid(Req req, Par param) {

        return params(req, param)
            .map(UUID::fromString)
            .findFirst();
    }

    private boolean isTrue(Req req, Par param) {
        return params(req, param).anyMatch(Boolean::parseBoolean);
    }

    private Handling redirect(Req req, boolean referer) {

        return handle(req, referer ? Netty.redirect(req.getReferer()) : Netty.redirect(Page.ADMIN));
    }

    private Optional<Session> closedSession(Req req) {

        return params(req, SESSION)
            .map(UUID::fromString)
            .flatMap(session ->
                sessions.close(session).stream())
            .peek(closed ->
                log.info("Closed session: {}", closed))
            .findFirst();
    }

    private Stream<String> params(Req req, Par param) {

        return new HttpPostRequestDecoder(req.getRequest()).getBodyHttpDatas().stream()
            .filter(isParam(param).and(MixedAttribute.class::isInstance))
            .map(MixedAttribute.class::cast)
            .map(MixedAttribute::content)
            .map(this::print)
            .filter(s -> !s.isBlank());
    }

    private Predicate<InterfaceHttpData> isParam(Par param) {

        return data -> data.getName().equals(param.getName());
    }

    private String print(ByteBuf c) {

        return c.toString(StandardCharsets.UTF_8);
    }

    private boolean is(Req req, String action) {

        return HttpMethod.POST.equals(req.getRequest().method())
            && req.getPath().contains('/' + action)
            && FORM_URLENCODED.equals(req.header("Content-Type"));
    }

    private String map(Ids ids) {

        try {
            return IO.OMP.writeValueAsString(ids.getACL());
        } catch (Exception e) {
            throw new IllegalStateException("No map", e);
        }
    }

    private Ids readIds(String ids) {

        try {
            return new Ids(IO.OM.readerFor(ACL.class).readValue(ids), s3);
        } catch (Exception e) {
            throw new IllegalStateException("No map", e);
        }
    }
}
