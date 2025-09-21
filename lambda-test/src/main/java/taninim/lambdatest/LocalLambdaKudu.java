package taninim.lambdatest;

import module java.base;
import module taninim.kudu;
import module taninim.taninim;
import module uplift.flambda;
import module uplift.kernel;
import module uplift.lambda;
import module uplift.s3;

@SuppressWarnings({"MagicNumber"})
public final class LocalLambdaKudu {

    static void main() {
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
