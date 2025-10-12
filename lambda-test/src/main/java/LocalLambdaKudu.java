import module java.base;
import module taninim.kudu;
import module taninim.taninim;
import module uplift.flambda;
import module uplift.kernel;
import module uplift.lambda;
import module uplift.s3;

@SuppressWarnings({"MagicNumber"})
void main() {
    var corsSettings = new CorsSettings(
        List.of("https://kjetilv.github.io"),
        List.of("GET"),
        List.of("content-type", "range")
    );

    Supplier<Instant> clock = Clock.systemUTC()::instant;
    var settings = new LocalLambdaSettings(
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
        LamdbdaManaged.create(localLambda.getLambdaUri(), clientSettings, handler);

    try (var executor = Executors.newFixedThreadPool(2)) {
        executor.submit(localLambda);
        executor.submit(lamdbdaManaged);
    }
}

private static final Env ENV = Env.actual();
