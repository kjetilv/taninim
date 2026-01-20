package taninim.yellin.server;

import module java.base;
import com.github.kjetilv.uplift.kernel.Env;
import com.github.kjetilv.uplift.s3.S3Accessor;
import com.github.kjetilv.uplift.synchttp.HttpCallbackProcessor;
import com.github.kjetilv.uplift.synchttp.Server;
import com.github.kjetilv.uplift.util.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taninim.fb.DefaultFbAuthenticator;
import taninim.yellin.Yellin;

import java.lang.management.ManagementFactory;

import static com.github.kjetilv.uplift.util.MainSupport.*;

public record ServerYellin(Map<String, String> parameters) implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ServerYellin.class);

    public static final int PORT_80 = 80;

    @Override
    public void run() {
        var s3Accessor = S3Accessor.fromEnvironment(
            Env.actual(),
            Executors.newVirtualThreadPerTaskExecutor()
        );

        HttpCallbackProcessor.HttpHandler httpHandler = new YellinSyncChannelHandler(
            Yellin.leasesDispatcher(
                s3Accessor,
                Time.UTC_CLOCK::instant,
                Duration.ofDays(1),
                Duration.ofHours(1),
                new DefaultFbAuthenticator()
            ));

        Server.Processor processor = new HttpCallbackProcessor(
            httpHandler,
            Arena.ofAuto(),
            MAX_REQUEST_SIZE
        );

        var port = resolvePort();
        try (var server = Server.create(port).run(processor)) {
            var uptime = Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime());
            log.info("Yellin startup @ {}", uptime);
            server.join();
        }
    }

    private int resolvePort() {
        return validatePort(possibleIntArg(parameters, "port").orElse(PORT_80));
    }
}
