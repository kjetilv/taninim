package taninim.kudu.server;

import module java.base;
import com.github.kjetilv.uplift.synchttp.Servers;
import com.github.kjetilv.uplift.util.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taninim.music.legal.ArchivedLeasesRegistry;
import taninim.music.legal.CloudMediaLibrary;
import taninim.music.legal.S3Archives;
import taninim.server.AbstractServer;

final class KuduServer extends AbstractServer {

    private static final Logger log = LoggerFactory.getLogger(KuduServer.class);

    static void main(String[] args) {
        new KuduServer(args).run();
    }

    private KuduServer(String... args) {
        super("kudu", args);
    }

    @Override
    public void run() {
        var leasesRegistry = ArchivedLeasesRegistry.create(
            S3Archives.create(s3Accessor()),
            Duration.ofHours(1),
            Time.utcSupplier()
        );

        var mediaLibrary = CloudMediaLibrary.create(s3Accessor(), Time.utcSupplier());

        var kudu = taninim.kudu.Kudu.create(
            leasesRegistry,
            mediaLibrary,
            parameters().buffer(),
            Time.utcSupplier()
        );

        Servers.await("Kudu", parameters().port(), new KuduHttpHandler(kudu))
            .onOK(closed ->
                log.info("Kudu closed: {}", closed))
            .onDenied(address ->
                log.error("Could not bind to {}", address));
    }
}
