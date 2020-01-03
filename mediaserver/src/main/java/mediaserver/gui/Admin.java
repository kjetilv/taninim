package mediaserver.gui;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.MixedAttribute;
import mediaserver.http.*;
import mediaserver.sessions.Ids;
import mediaserver.sessions.Sessions;
import mediaserver.util.IO;
import mediaserver.util.OnceEvery;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class Admin extends TemplateEnabled {

    public static final String FORM_URLENCODED = "application/x-www-form-urlencoded";

    private final Supplier<Ids> ids;

    private final Sessions sessions;

    public Admin(Supplier<Ids> ids, Sessions sessions, Templater templater) {

        super(templater, Page.ADMIN);
        this.ids = ids;
        this.sessions = sessions;
    }

    @Override
    public Handling handleRequest(WebPath webPath) {

        FullHttpRequest request = webPath.getRequest();
        if (isIdsUpdate(webPath, request)) {
            ids(webPath).ifPresent(Ids::persist);
            OnceEvery.refresh(ids);
            return respondPath(webPath, Netty.redirectResponse(Page.ADMIN));
        }

        return Optional.of(adminTemplate()
            .add(QPar.USER, webPath.getSession().getActiveUser())
            .add(QPar.SESSIONS, sessions.list())
            .add(QPar.IDS, map(ids.get())))
            .map(template ->
                respondHtml(webPath, template))
            .orElseGet(() ->
                handleNotFound(webPath));
    }

    private Optional<Ids> ids(WebPath webPath) {

        return new HttpPostRequestDecoder(webPath.getRequest()).getBodyHttpDatas().stream()
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

    private boolean isIdsUpdate(WebPath webPath, FullHttpRequest request) {

        return HttpMethod.POST.equals(request.method()) && FORM_URLENCODED.equals(webPath.header("Content-Type"));
    }

    private String map(Ids ids) {

        try {
            return IO.OMP.writeValueAsString(ids.toMap());
        } catch (Exception e) {
            throw new IllegalStateException("No map", e);
        }
    }

    private Ids map(String ids) {

        try {
            return new Ids(IO.OM.readerFor(Map.class).readValue(ids));
        } catch (Exception e) {
            throw new IllegalStateException("No map", e);
        }
    }
}
