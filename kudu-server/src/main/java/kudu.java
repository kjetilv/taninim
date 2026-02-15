import module java.base;
import com.github.kjetilv.uplift.kernel.Env;
import com.github.kjetilv.uplift.s3.S3Accessor;
import com.github.kjetilv.uplift.synchttp.HttpCallbackProcessor;
import com.github.kjetilv.uplift.synchttp.Server;
import com.github.kjetilv.uplift.util.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taninim.kudu.DefaultKudu;
import taninim.kudu.server.KuduHttpHandler;
import taninim.music.legal.ArchivedLeasesRegistry;
import taninim.music.legal.CloudMediaLibrary;
import taninim.music.legal.S3Archives;

import java.lang.management.ManagementFactory;

import static com.github.kjetilv.uplift.util.MainSupport.*;

private static final Logger log = LoggerFactory.getLogger("kudu");

void main(String[] args) {
    var parameters = Parameters.parse(args);

    var executorService = Executors.newVirtualThreadPerTaskExecutor();
    var s3Accessor = S3Accessor.fromEnvironment(Env.actual(), executorService);

    var leasesRegistry = ArchivedLeasesRegistry.create(
        S3Archives.create(s3Accessor),
        Duration.ofHours(1),
        Time.utcSupplier()
    );

    var mediaLibrary = CloudMediaLibrary.create(s3Accessor, Time.utcSupplier());

    var kudu = DefaultKudu.create(
        leasesRegistry,
        mediaLibrary,
        parameters.buffer(),
        Time.utcSupplier()
    );
    var handler = new KuduHttpHandler(kudu);

    try (var server = Server.create(parameters.port()).run(new HttpCallbackProcessor(handler))) {
        var uptime = Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime());
        log.info("Kudu startup @ {}", uptime);
        server.join();
    }
}

private record Parameters(int port, int buffer, boolean server) {

    private static Parameters parse(String[] args) {
        var map = parameterMap(args);
        var server = boolArg(map, "server") || possibleIntArg(map, "port").isPresent();
        return new Parameters(
            validatePort(intArg(map, "port", server ? PORT_80 : 0)),
            intArg(map, "buffer", DEFAULT_RESPONSE_LENGTH),
            server
        );
    }

    private static final int PORT_80 = 80;

    private static final int DEFAULT_RESPONSE_LENGTH = 64 * 1_024;
}
