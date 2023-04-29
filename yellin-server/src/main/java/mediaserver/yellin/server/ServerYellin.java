package mediaserver.yellin.server;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import com.github.kjetilv.uplift.asynchttp.BufferState;
import com.github.kjetilv.uplift.asynchttp.ChannelHandler;
import com.github.kjetilv.uplift.asynchttp.IOServer;
import com.github.kjetilv.uplift.asynchttp.MainSupport;
import com.github.kjetilv.uplift.asynchttp.ServerRunner;
import com.github.kjetilv.uplift.kernel.Env;
import mediaserver.fb.FbAuthenticator;
import com.github.kjetilv.uplift.json.Json;
import com.github.kjetilv.uplift.s3.S3Accessor;
import mediaserver.yellin.ActivationSerializer;
import mediaserver.yellin.LeasesDispatcher;

import static com.github.kjetilv.uplift.asynchttp.MainSupport.MAX_REQUEST_SIZE;
import static com.github.kjetilv.uplift.asynchttp.MainSupport.possibleIntArg;
import static com.github.kjetilv.uplift.asynchttp.MainSupport.validatePort;
import static com.github.kjetilv.uplift.asynchttp.ServerRunner.create;
import static com.github.kjetilv.uplift.kernel.ManagedExecutors.executor;
import static com.github.kjetilv.uplift.kernel.Time.UTC_CLOCK;
import static mediaserver.yellin.Yellin.activationSerializer;
import static mediaserver.yellin.Yellin.leasesDispatcher;

public final class ServerYellin {

    public static void main(String[] args) {
        Map<String, String> map = MainSupport.parameterMap(args);
        Optional<Integer> portArg = possibleIntArg(map, "port");
        ExecutorService executorService = executor("SL");
        S3Accessor s3Accessor = S3Accessor.fromEnvironment(Env.actual(), executorService);

        int port = validatePort(portArg.orElse(MainSupport.DEFAULT_PORT));
        startServer(port, s3Accessor, executorService, activationSerializer(s3Accessor)).join();
    }

    private ServerYellin() {
    }

    private static final Duration SESSION_DURATION = Duration.ofDays(1);

    private static final Duration LEASE_DURATION = Duration.ofHours(1);

    private static IOServer startServer(
        int port, S3Accessor s3Accessor, ExecutorService executor, ActivationSerializer activationSerializer
    ) {
        try (
            ServerRunner serverRunner = create(port, MAX_REQUEST_SIZE, executor)
        ) {
            LeasesDispatcher leasesDispatcher =
                leasesDispatcher(
                    s3Accessor,
                    executor,
                    UTC_CLOCK::instant,
                    SESSION_DURATION,
                    LEASE_DURATION,
                    new FbAuthenticator(Json.STRING_2_JSON_MAP)
                );
            ChannelHandler<BufferState, YellinChannelHandler> handler =
                new YellinChannelHandler(
                    leasesDispatcher,
                    Json.STRING_2_JSON_MAP,
                    activationSerializer::jsonBody,
                    null,
                    MAX_REQUEST_SIZE,
                    UTC_CLOCK::instant
                );
            return serverRunner.run(handler);
        }
    }
}
