package mediaserver.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CookiesPlease extends NettyHandler {

    private static final Logger log = LoggerFactory.getLogger(CookiesPlease.class);

    public CookiesPlease() {

        super(Page.COOKIESPLEASE);
    }

    @Override
    public Handling handleRequest(WebPath webPath) {

        return respond(webPath, Netty.okCookieResponse(webPath, "cookies"));
    }
}
