package taninim.yellin.server;

import module java.base;
import com.github.kjetilv.uplift.synchttp.HttpHandler;
import com.github.kjetilv.uplift.synchttp.Servers;
import com.github.kjetilv.uplift.util.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taninim.fb.DefaultFbAuthenticator;
import taninim.server.AbstractServer;
import taninim.yellin.DefaultYellin;

final class YellinServer extends AbstractServer {

    private static final Logger log = LoggerFactory.getLogger(YellinServer.class);

    static void main(String[] args) {
        new YellinServer(args).run();
    }

    private YellinServer(String... args) {
        super("yellin", args);
    }

    @Override
    public void run() {

        HttpHandler handler = new YellinHttpHandler(
            DefaultYellin.create(
                s3Accessor(),
                Time.UTC_CLOCK::instant,
                Duration.ofDays(1),
                Duration.ofHours(1),
                new DefaultFbAuthenticator()
            ));

        Servers.await("Yellin", parameters().port(), handler)

            .onOK(closed ->
                log.info("Yellin closed: {}", closed))
            .onDenied(address ->
                log.error("Could not bind to {}", address));
    }
}
