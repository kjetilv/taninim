package taninim.kudu.server;

import module java.base;
import com.github.kjetilv.uplift.kernel.Env;
import com.github.kjetilv.uplift.s3.S3Accessor;
import com.github.kjetilv.uplift.util.Time;
import taninim.kudu.DefaultKudu;
import taninim.music.legal.ArchivedLeasesRegistry;
import taninim.music.legal.CloudMediaLibrary;
import taninim.music.legal.S3Archives;

import static com.github.kjetilv.uplift.asynchttp.AsyncServerRunner.create;

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

        var handler = KuduAsyncChannelHandler.create(
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
