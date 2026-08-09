import module java.base;
import com.github.kjetilv.uplift.flambda.Flambda;
import com.github.kjetilv.uplift.flambda.FlambdaSettings;
import com.github.kjetilv.uplift.flogs.LogLevel;
import com.github.kjetilv.uplift.kernel.Env;
import com.github.kjetilv.uplift.lambda.Lambda;
import com.github.kjetilv.uplift.lambda.LambdaClientSettings;
import com.github.kjetilv.uplift.s3.S3AccessorFactory;
import com.github.kjetilv.uplift.synchttp.CorsSettings;
import org.slf4j.Logger;
import taninim.TaninimSettings;
import taninim.kudu.Kudu;
import taninim.kudu.KuduLambdaHandler;
import taninim.util.VirtualRun;

import static com.github.kjetilv.uplift.flogs.Flogs.initializeAndGet;

@SuppressWarnings({"MagicNumber"})
void main() {
    var corsSettings = new CorsSettings(
        List.of("https://kjetilv.github.io"),
        List.of("GET"),
        List.of("content-type", "range")
    );
    Supplier<Instant> clock = Clock.systemUTC()::instant;
    try (
        var flambda = new Flambda(
            new FlambdaSettings(
                "kudu",
                9002,
                8080,
                8 * 8192,
                10,
                corsSettings,
                clock
            ))
    ) {
        var clientSettings = new LambdaClientSettings(ENV, clock);
        var taninimSettings = new TaninimSettings(
            Duration.ofDays(1),
            Duration.ofHours(4),
            1024 * 1024
        );
        var handler = new KuduLambdaHandler(Kudu.create(
            clientSettings,
            taninimSettings,
            S3AccessorFactory.defaultFactory(ENV)
        ));

        VirtualRun.join(
            "kudu",
            () -> {
                try (
                    var lamdbdaManaged = Lambda.managed(flambda.lambdaUri(), clientSettings, handler);
                    var executor = Executors.newFixedThreadPool(2)
                ) {
                    executor.submit(() -> lamdbdaManaged.accept("kudu"));
                }
            }
        );
    }
}

private static final Logger logger = initializeAndGet("localLambdaKudu", LogLevel.DEBUG);

private static final Env ENV = Env.actual();
