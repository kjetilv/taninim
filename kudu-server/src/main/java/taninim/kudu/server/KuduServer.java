package taninim.kudu.server;

import module java.base;
import com.github.kjetilv.uplift.kernel.Env;
import com.github.kjetilv.uplift.s3.S3Accessor;
import com.github.kjetilv.uplift.synchttp.Servers;
import com.github.kjetilv.uplift.util.Time;
import com.github.kjetilv.uplift.util.Virtuals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taninim.music.legal.ArchivedLeasesRegistry;
import taninim.music.legal.CloudMediaLibrary;
import taninim.music.legal.S3Archives;

import static com.github.kjetilv.uplift.util.MainSupport.*;

public class KuduServer {

    private static final Logger log = LoggerFactory.getLogger(KuduServer.class);

    void main(String[] args) {
        var parameters = Parameters.parse(args);

        var executorService = Virtuals.executor("kudu");
        var s3Accessor = S3Accessor.fromEnvironment(Env.actual(), executorService);

        var leasesRegistry = ArchivedLeasesRegistry.create(
            S3Archives.create(s3Accessor),
            Duration.ofHours(1),
            Time.utcSupplier()
        );

        var mediaLibrary = CloudMediaLibrary.create(s3Accessor, Time.utcSupplier());

        var kudu = taninim.kudu.Kudu.create(
            leasesRegistry,
            mediaLibrary,
            parameters.buffer(),
            Time.utcSupplier()
        );

        Servers.await("Kudu", parameters.port(), new KuduHttpHandler(kudu))
            .onOK(closed ->
                log.info("Kudu closed: {}", closed))
            .onDenied(address ->
                log.error("Could not bind to {}", address));
    }

    private record Parameters(int port, int buffer, boolean server) {

        private static final int PORT_80 = 80;

        private static final int DEFAULT_RESPONSE_LENGTH = 64 * 1_024;

        private static Parameters parse(String[] args) {
            var map = parameterMap(args);
            var server = boolArg(map, "server") || possibleIntArg(map, "port").isPresent();
            return new Parameters(
                validatePort(intArg(map, "port", server ? PORT_80 : 0)),
                intArg(map, "buffer", DEFAULT_RESPONSE_LENGTH),
                server
            );
        }
    }
}
