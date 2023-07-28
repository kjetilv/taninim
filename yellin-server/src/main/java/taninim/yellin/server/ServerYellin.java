package taninim.yellin.server;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import com.github.kjetilv.uplift.asynchttp.BufferState;
import com.github.kjetilv.uplift.asynchttp.ChannelHandler;
import com.github.kjetilv.uplift.asynchttp.IOServer;
import com.github.kjetilv.uplift.asynchttp.MainSupport;
import com.github.kjetilv.uplift.json.Json;
import com.github.kjetilv.uplift.kernel.Env;
import com.github.kjetilv.uplift.s3.S3Accessor;
import taninim.fb.FbAuthenticator;

import static com.github.kjetilv.uplift.asynchttp.MainSupport.MAX_REQUEST_SIZE;
import static com.github.kjetilv.uplift.asynchttp.MainSupport.possibleIntArg;
import static com.github.kjetilv.uplift.asynchttp.MainSupport.validatePort;
import static com.github.kjetilv.uplift.asynchttp.ServerRunner.create;
import static com.github.kjetilv.uplift.kernel.ManagedExecutors.executor;
import static com.github.kjetilv.uplift.kernel.Time.UTC_CLOCK;
import static taninim.yellin.Yellin.activationSerializer;
import static taninim.yellin.Yellin.leasesDispatcher;

public final class ServerYellin {

    public static void main(String[] args) {
        Map<String, String> map = MainSupport.parameterMap(args);
        ExecutorService executorService = executor("SL");
        S3Accessor s3Accessor = S3Accessor.fromEnvironment(Env.actual(), executorService);
        ChannelHandler<BufferState, YellinChannelHandler> handler = handler(s3Accessor, executorService);
        try (IOServer server = create(port(map), MAX_REQUEST_SIZE, executorService).run(handler)) {
            server.join();
        }
    }

    private ServerYellin() {
    }

    private static final Duration SESSION_DURATION = Duration.ofDays(1);

    private static final Duration LEASE_DURATION = Duration.ofHours(1);

    public static final int PORT_80 = 80;

    private static int port(Map<String, String> map) {
        Optional<Integer> portArg = possibleIntArg(map, "port");
        return validatePort(portArg.orElse(PORT_80));
    }

    private static ChannelHandler<BufferState, YellinChannelHandler> handler(
        S3Accessor s3Accessor,
        ExecutorService executorService
    ) {
        return new YellinChannelHandler(
            leasesDispatcher(
                s3Accessor,
                executorService,
                UTC_CLOCK::instant,
                SESSION_DURATION,
                LEASE_DURATION,
                new FbAuthenticator(Json.STRING_2_JSON_MAP)
            ),
            Json.STRING_2_JSON_MAP,
            activationSerializer(s3Accessor)::jsonBody,
            null,
            MAX_REQUEST_SIZE,
            UTC_CLOCK::instant
        );
    }
}
