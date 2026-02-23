import module java.base;
import com.github.kjetilv.uplift.kernel.Env;
import com.github.kjetilv.uplift.s3.S3Accessor;
import com.github.kjetilv.uplift.synchttp.HttpCallbackProcessor;
import com.github.kjetilv.uplift.synchttp.HttpHandler;
import com.github.kjetilv.uplift.synchttp.Server;
import com.github.kjetilv.uplift.util.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taninim.fb.DefaultFbAuthenticator;
import taninim.yellin.DefaultYellin;
import taninim.yellin.server.YellinHttpHandler;

import java.lang.management.ManagementFactory;

import static com.github.kjetilv.uplift.util.MainSupport.*;

private static final Logger log = LoggerFactory.getLogger("yellin");

void main(String[] args) {

    var parameters = parameterMap(args);

    var s3Accessor = S3Accessor.fromEnvironment(
        Env.actual(),
        Executors.newVirtualThreadPerTaskExecutor()
    );

    HttpHandler httpHandler = new YellinHttpHandler(
        DefaultYellin.create(
            s3Accessor,
            Time.UTC_CLOCK::instant,
            Duration.ofDays(1),
            Duration.ofHours(1),
            new DefaultFbAuthenticator()
        ));

    Server.Processor processor = new HttpCallbackProcessor(httpHandler, Arena.ofAuto());

    var port = resolvePort(parameters);
    try (var server = Server.create(port).run(processor)) {
        var uptime = Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime());
        log.info("Yellin startup @ {}", uptime);
        server.join();
    }
}

private int resolvePort(Map<String, String > parameters) {
    return validatePort(possibleIntArg(parameters, "port").orElse(8080));
}
