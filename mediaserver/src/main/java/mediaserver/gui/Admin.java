package mediaserver.gui;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.MixedAttribute;
import mediaserver.externals.ACL;
import mediaserver.externals.S3;
import mediaserver.http.Handling;
import mediaserver.http.Page;
import mediaserver.http.Req;
import mediaserver.sessions.Ids;
import mediaserver.sessions.Session;
import mediaserver.sessions.Sessions;
import mediaserver.templates.TPar;
import mediaserver.toolkit.Templater;
import mediaserver.util.IO;
import mediaserver.util.OnceEvery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class Admin extends TemplateEnabled {

    public static final String FORM_URLENCODED = "application/x-www-form-urlencoded";

    private static final Logger log = LoggerFactory.getLogger(Admin.class);

    private static final String ACTION_IDS = "ids";

    private static final String ACTION_EXTERMINATE = "exterminate";

    private final Supplier<Ids> ids;

    private final Sessions sessions;

    private final S3.Client s3;

    private static final String PARAM_IDS = "ids";

    private static final String PARAM_SESSION = "session";

    public Admin(Supplier<Ids> ids, Sessions sessions, Templater templater, S3.Client s3) {

        super(templater, Page.ADMIN);
        this.ids = ids;
        this.sessions = sessions;
        this.s3 = s3;
    }

    @Override
    protected Handling handleRequest(Req req) {

        if (is(req, ACTION_IDS)) {
            if (s3 != null) {
                try {
                    params(req, PARAM_IDS)
                        .map(this::readIds)
                        .forEach(Ids::persist);
                } finally {
                    OnceEvery.refresh(ids);
                }
            } else {
                log.warn("Could not update, no S3 connection: {}", req);
            }
            return redirect(req, Page.ADMIN);
        }

        if (is(req, ACTION_EXTERMINATE)) {
            return closedSession(req)
                .map(closed ->
                    redirect(req, Page.ADMIN))
                .orElseGet(() ->
                    handleNotFound(req));
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

    private Optional<Session> closedSession(Req req) {

        return params(req, PARAM_SESSION)
            .map(UUID::fromString)
            .flatMap(session ->
                sessions.close(session).stream())
            .peek(closed -> {
                log.info("Closed session: {}", closed);
            })
            .findFirst();
    }

    private Stream<String> params(Req req, String param) {

        return new HttpPostRequestDecoder(req.getRequest()).getBodyHttpDatas().stream()
            .filter(isParam(param).and(MixedAttribute.class::isInstance))
            .map(MixedAttribute.class::cast)
            .map(MixedAttribute::content)
            .map(this::print);
    }

    private Predicate<InterfaceHttpData> isParam(String param) {

        return data -> data.getName().equals(param);
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
