import module java.base;
import com.github.kjetilv.uplift.flambda.CorsSettings;
import com.github.kjetilv.uplift.flambda.LocalLambda;
import com.github.kjetilv.uplift.flambda.FlambdaSettings;
import com.github.kjetilv.uplift.flogs.LogLevel;
import com.github.kjetilv.uplift.kernel.Env;
import com.github.kjetilv.uplift.lambda.Lambda;
import com.github.kjetilv.uplift.lambda.LambdaClientSettings;
import com.github.kjetilv.uplift.s3.S3AccessorFactory;
import taninim.TaninimSettings;
import taninim.kudu.KuduLambdaHandler;

import static com.github.kjetilv.uplift.flogs.Flogs.initialize;

@SuppressWarnings({"MagicNumber"})
void main() {
    initialize(LogLevel.DEBUG);

    var corsSettings = new CorsSettings(
        List.of("https://kjetilv.github.io"),
        List.of("GET"),
        List.of("content-type", "range")
    );

    Supplier<Instant> clock = Clock.systemUTC()::instant;
    var settings = new FlambdaSettings(
        9002,
        8080,
        8 * 8192,
        10,
        corsSettings,
        clock
    );

    var localLambda = new LocalLambda(settings);
    var clientSettings = new LambdaClientSettings(ENV, clock);
    var taninimSettings = new TaninimSettings(
        Duration.ofDays(1),
        Duration.ofHours(4),
        1024 * 1024
    );
    var handler = KuduLambdaHandler.create(
        clientSettings,
        taninimSettings,
        S3AccessorFactory.defaultFactory(ENV)
    );

    var lamdbdaManaged =
        Lambda.managed(localLambda.getLambdaUri(), clientSettings, handler);

    try (var executor = Executors.newFixedThreadPool(2)) {
        executor.submit(localLambda);
        executor.submit(lamdbdaManaged);
    }
}

private static final Env ENV = Env.actual();
