package taninim.kudu.server;

import module java.base;
import module taninim.kudu;
import module taninim.taninim;
import module uplift.asynchttp;
import module uplift.kernel;
import module uplift.s3;
import module uplift.util;

import static com.github.kjetilv.uplift.asynchttp.MainSupport.*;
import static com.github.kjetilv.uplift.asynchttp.ServerRunner.create;

public final class ServerKudu {

    static void main(String[] args) {
        Parameters parameters = parameters(args);
        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
        S3Accessor s3Accessor = S3Accessor.fromEnvironment(Env.actual(), executorService);

        LeasesRegistry leasesRegistry = new ArchivedLeasesRegistry(
            new S3Archives(s3Accessor),
            Duration.ofHours(1),
            Time.utcSupplier()
        );

        MediaLibrary mediaLibrary =
            new CloudMediaLibrary(s3Accessor, Time.utcSupplier());

        ChannelHandler<StreamingState, KuduChannelHandler> handler = new KuduChannelHandler(
            new DefaultKudu(leasesRegistry, mediaLibrary, parameters.buffer(), Time.utcSupplier()),
            MAX_REQUEST_LENGTH,
            parameters.buffer(),
            Time.utcSupplier()
        );

        try (
            IOServer run = create(parameters.port(), MAX_REQUEST_LENGTH).run(handler)
        ) {
            run.join();
        }
    }

    private ServerKudu() {
    }

    private static final int PORT_80 = 80;

    private static final int MAX_REQUEST_LENGTH = 1024;

    private static final int DEFAULT_RESPONSE_LENGTH = 64 * 1_024;

    private static Parameters parameters(String[] args) {
        Map<String, String> map = parameterMap(args);
        boolean server = boolArg(map, "server") || possibleIntArg(map, "port").isPresent();
        return new Parameters(
            validatePort(intArg(map, "port", server ? PORT_80 : 0)),
            intArg(map, "buffer", DEFAULT_RESPONSE_LENGTH),
            server
        );
    }
}
