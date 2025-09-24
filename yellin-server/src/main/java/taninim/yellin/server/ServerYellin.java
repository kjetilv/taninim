package taninim.yellin.server;

import module java.base;
import module taninim.fb;
import module uplift.asynchttp;
import module uplift.s3;
import module uplift.util;
import taninim.yellin.Yellin;

import static com.github.kjetilv.uplift.asynchttp.MainSupport.*;
import static com.github.kjetilv.uplift.asynchttp.ServerRunner.create;

public record ServerYellin(Map<String, String> parameters) implements Runnable {

    public static final int PORT_80 = 80;

    @Override
    public void run() {
        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
        S3Accessor s3Accessor = S3Accessor.fromEnvironment(Env.actual(), executorService);
        ChannelHandler<BufferState, YellinChannelHandler> handler = new YellinChannelHandler(
            Yellin.leasesDispatcher(
                s3Accessor,
                Time.UTC_CLOCK::instant,
                Duration.ofDays(1),
                Duration.ofHours(1),
                FbAuthenticator.simple()
            ),
            null,
            MAX_REQUEST_SIZE,
            Time.UTC_CLOCK::instant
        );
        int port = validatePort(possibleIntArg(parameters, "port").orElse(PORT_80));
        try (IOServer server = create(port, MAX_REQUEST_SIZE).run(handler)) {
            server.join();
        }
    }
}
