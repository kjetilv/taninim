package taninim.yellin.server;

import module java.base;
import com.github.kjetilv.uplift.asynchttp.BufferState;
import com.github.kjetilv.uplift.asynchttp.ChannelHandler;
import com.github.kjetilv.uplift.kernel.Env;
import com.github.kjetilv.uplift.s3.S3Accessor;
import com.github.kjetilv.uplift.util.Time;
import taninim.fb.FbAuthenticator;
import taninim.yellin.Yellin;

import static com.github.kjetilv.uplift.asynchttp.MainSupport.*;
import static com.github.kjetilv.uplift.asynchttp.ServerRunner.create;

public record ServerYellin(Map<String, String> parameters) implements Runnable {

    public static final int PORT_80 = 80;

    @Override
    public void run() {
        var executorService = Executors.newVirtualThreadPerTaskExecutor();
        var s3Accessor = S3Accessor.fromEnvironment(Env.actual(), executorService);
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
        var port = validatePort(possibleIntArg(parameters, "port").orElse(PORT_80));
        try (var server = create(port, MAX_REQUEST_SIZE).run(handler)) {
            server.join();
        }
    }
}
