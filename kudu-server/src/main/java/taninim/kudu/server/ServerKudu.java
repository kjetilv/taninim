package taninim.kudu.server;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import com.github.kjetilv.uplift.asynchttp.ChannelHandler;
import com.github.kjetilv.uplift.asynchttp.IOServer;
import com.github.kjetilv.uplift.kernel.Env;
import com.github.kjetilv.uplift.kernel.Time;
import com.github.kjetilv.uplift.s3.S3Accessor;
import taninim.kudu.DefaultKudu;
import taninim.music.LeasesRegistry;
import taninim.music.legal.ArchivedLeasesRegistry;
import taninim.music.legal.CloudMediaLibrary;
import taninim.music.legal.S3Archives;
import taninim.music.medias.MediaLibrary;

import static com.github.kjetilv.uplift.asynchttp.MainSupport.boolArg;
import static com.github.kjetilv.uplift.asynchttp.MainSupport.intArg;
import static com.github.kjetilv.uplift.asynchttp.MainSupport.parameterMap;
import static com.github.kjetilv.uplift.asynchttp.MainSupport.possibleIntArg;
import static com.github.kjetilv.uplift.asynchttp.MainSupport.validatePort;
import static com.github.kjetilv.uplift.asynchttp.ServerRunner.create;
import static com.github.kjetilv.uplift.kernel.ManagedExecutors.executor;

public final class ServerKudu {

    public static void main(String[] args) {
        Parameters parameters = parameters(args);
        ExecutorService executorService = executor("SL");
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
            IOServer run = create(parameters.port(), MAX_REQUEST_LENGTH, executorService).run(handler)
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
