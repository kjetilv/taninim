package taninim.yellin.server;

import module java.base;
import module taninim.fb;
import module uplift.asynchttp;
import module uplift.kernel;
import module uplift.s3;

import static com.github.kjetilv.uplift.asynchttp.MainSupport.*;
import static com.github.kjetilv.uplift.asynchttp.ServerRunner.create;
import static com.github.kjetilv.uplift.util.Time.UTC_CLOCK;
import static taninim.yellin.Yellin.leasesDispatcher;

public final class ServerYellin {

    static void main(String[] args) {
        Map<String, String> map = parameterMap(args);
        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
        S3Accessor s3Accessor = S3Accessor.fromEnvironment(Env.actual(), executorService);
        ChannelHandler<BufferState, YellinChannelHandler> handler = handler(s3Accessor);
        try (IOServer server = create(port(map), MAX_REQUEST_SIZE).run(handler)) {
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

    private static ChannelHandler<BufferState, YellinChannelHandler> handler(S3Accessor s3Accessor) {
        return new YellinChannelHandler(
            leasesDispatcher(
                s3Accessor,
                UTC_CLOCK::instant,
                SESSION_DURATION,
                LEASE_DURATION,
                FbAuthenticator.simple()
            ),
            null,
            MAX_REQUEST_SIZE,
            UTC_CLOCK::instant
        );
    }
}
