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
        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
        S3Accessor s3Accessor = S3Accessor.fromEnvironment(Env.actual(), executorService);

        LeasesRegistry leasesRegistry = new ArchivedLeasesRegistry(
            new S3Archives(s3Accessor),
            Duration.ofHours(1),
            Time.utcSupplier()
        );

        MediaLibrary mediaLibrary = new CloudMediaLibrary(s3Accessor, Time.utcSupplier());

        ChannelHandler<StreamingState, KuduChannelHandler> handler = new KuduChannelHandler(
            new DefaultKudu(leasesRegistry, mediaLibrary, parameters.buffer(), Time.utcSupplier()),
            MAX_REQUEST_LENGTH,
            parameters.buffer(),
            Time.utcSupplier()
        );

        try (IOServer run = create(parameters.port(), MAX_REQUEST_LENGTH).run(handler)) {
            run.join();
        }
    }

    private static final int MAX_REQUEST_LENGTH = 1024;
}
