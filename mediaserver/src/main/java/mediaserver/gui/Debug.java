package mediaserver.gui;

import mediaserver.http.Handling;
import mediaserver.http.NettyHandler;
import mediaserver.http.Page;
import mediaserver.http.WebPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Debug extends NettyHandler {

    private static final Logger log = LoggerFactory.getLogger(Debug.class);

    public Debug() {

        super(Page.DEBUG);
    }

    @Override
    public Handling handleRequest(WebPath webPath) {

        log.info("Request receieved @ {}: {}", webPath, webPath.getRequest());
        return pass();
    }
}
