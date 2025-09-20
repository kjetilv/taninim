package taninim.lambdatest;

import com.github.kjetilv.uplift.flambda.CorsSettings;
import com.github.kjetilv.uplift.flambda.LocalLambda;
import com.github.kjetilv.uplift.flambda.LocalLambdaSettings;
import com.github.kjetilv.uplift.kernel.Env;
import com.github.kjetilv.uplift.lambda.LambdaClientSettings;
import com.github.kjetilv.uplift.lambda.LambdaHandler;
import com.github.kjetilv.uplift.lambda.LamdbdaManaged;
import com.github.kjetilv.uplift.s3.S3AccessorFactory;
import taninim.TaninimSettings;
import taninim.kudu.KuduLambdaHandler;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

@SuppressWarnings({"MagicNumber"})
public final class LocalLambdaKudu {

    static void main(String[] args) {
        CorsSettings corsSettings = new CorsSettings(
            List.of("https://kjetilv.github.io"),
            List.of("GET"),
            List.of("content-type", "range")
        );

        Supplier<Instant> clock = Clock.systemUTC()::instant;
        LocalLambdaSettings settings = new LocalLambdaSettings(
            9002,
            8080,
            8 * 8192,
            10,
            corsSettings,
            clock
        );

        LocalLambda localLambda = new LocalLambda(settings);

        LambdaClientSettings clientSettings =
            new LambdaClientSettings(ENV, clock);

        TaninimSettings taninimSettings = new TaninimSettings(
            Duration.ofDays(1),
            Duration.ofHours(4),
            1024 * 1024
        );

        LambdaHandler handler = KuduLambdaHandler.create(
            clientSettings,
            taninimSettings,
            S3AccessorFactory.defaultFactory(ENV)
        );

        Runnable lamdbdaManaged =
            LamdbdaManaged.create(localLambda.getLambdaUri(), clientSettings, handler);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            executor.submit(localLambda);
            executor.submit(lamdbdaManaged);
        }
    }

    private LocalLambdaKudu() {
    }

    private static final Env ENV = Env.actual();
}
