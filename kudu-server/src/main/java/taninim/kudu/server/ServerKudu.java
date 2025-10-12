package taninim.kudu.server;

import module java.base;
import module taninim.kudu;
import module taninim.taninim;
import module uplift.asynchttp;
import module uplift.kernel;
import module uplift.s3;
import module uplift.util;

import static com.github.kjetilv.uplift.asynchttp.ServerRunner.create;

public record ServerKudu(Parameters parameters) implements Runnable {

    @Override
    public void run() {
        var executorService = Executors.newVirtualThreadPerTaskExecutor();
        var s3Accessor = S3Accessor.fromEnvironment(Env.actual(), executorService);

        var leasesRegistry = ArchivedLeasesRegistry.create(
            S3Archives.create(s3Accessor),
            Duration.ofHours(1),
            Time.utcSupplier()
        );

        var mediaLibrary = CloudMediaLibrary.create(s3Accessor, Time.utcSupplier());

        var handler = KuduChannelHandler.create(
            new DefaultKudu(
                leasesRegistry,
                mediaLibrary,
                parameters.buffer(),
                Time.utcSupplier()
            ),
            MAX_REQUEST_LENGTH,
            parameters.buffer(),
            Time.utcSupplier()
        );

        try (var run = create(parameters.port(), MAX_REQUEST_LENGTH).run(handler)) {
            run.join();
        }
    }

    private static final int MAX_REQUEST_LENGTH = 1024;
}
