package mediaserver.gui;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.MixedAttribute;
import mediaserver.externals.ACL;
import mediaserver.externals.S3;
import mediaserver.http.*;
import mediaserver.sessions.Ids;
import mediaserver.sessions.Sessions;
import mediaserver.templates.TPar;
import mediaserver.toolkit.Templater;
import mediaserver.util.IO;
import mediaserver.util.OnceEvery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Supplier;

public final class Admin extends TemplateEnabled {

    private static final Logger log = LoggerFactory.getLogger(Admin.class);

    public static final String FORM_URLENCODED = "application/x-www-form-urlencoded";

    private final Supplier<Ids> ids;

    private final Sessions sessions;

    private final S3.Client s3;

    public Admin(Supplier<Ids> ids, Sessions sessions, Templater templater, S3.Client s3) {

        super(templater, Page.ADMIN);
        this.ids = ids;
        this.sessions = sessions;
        this.s3 = s3;
    }

    @Override
    protected Handling handleRequest(Req req) {

        FullHttpRequest request = req.getRequest();
        if (isIdsUpdate(req, request)) {
            if (s3 != null) {
                ids(req).ifPresent(Ids::persist);
                OnceEvery.refresh(ids);
            } else {
                log.warn("Could not update, no S3 connection: {}", req);
            }
            return respond(req, Netty.redirectResponse(Page.ADMIN));
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

    private Optional<Ids> ids(Req req) {

        return new HttpPostRequestDecoder(req.getRequest()).getBodyHttpDatas().stream()
            .filter(data ->
                data.getName().equals("ids"))
            .filter(MixedAttribute.class::isInstance)
            .map(MixedAttribute.class::cast)
            .map(MixedAttribute::content)
            .map(content ->
                content.toString(StandardCharsets.UTF_8))
            .map(this::map)
            .findFirst();
    }

    private boolean isIdsUpdate(Req req, FullHttpRequest request) {

        return HttpMethod.POST.equals(request.method()) && FORM_URLENCODED.equals(req.header("Content-Type"));
    }

    private String map(Ids ids) {

        try {
            return IO.OMP.writeValueAsString(ids.getACL());
        } catch (Exception e) {
            throw new IllegalStateException("No map", e);
        }
    }

    private Ids map(String ids) {

        try {
            return new Ids(IO.OM.readerFor(ACL.class).readValue(ids), s3);
        } catch (Exception e) {
            throw new IllegalStateException("No map", e);
        }
    }
}
