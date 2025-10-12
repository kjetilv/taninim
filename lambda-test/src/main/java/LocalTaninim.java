import module java.base;
import module taninim.fb;
import module taninim.kudu;
import module taninim.taninim;
import module taninim.yellin;
import module uplift.flambda;
import module uplift.flogs;
import module uplift.kernel;
import module uplift.lambda;
import module uplift.s3;
import module uplift.util;

import static com.github.kjetilv.uplift.flogs.Flogs.initialize;
import static com.github.kjetilv.uplift.flogs.LogLevel.DEBUG;
import static com.github.kjetilv.uplift.util.Time.utcSupplier;

void main() {
    initialize(DEBUG);
    var logger = LoggerFactory.getLogger("LocalTaninim");
    var time = utcSupplier();

    var taninimSettings = new TaninimSettings(ONE_DAY, FOUR_HOURS, K * K);

    var kuduCors = new CorsSettings(
        List.of("https://kjetilv.github.io"),
        List.of("GET"),
        List.of("content-type", "range")
    );
    var yellinCors = new CorsSettings(
        List.of("https://kjetilv.github.io"),
        List.of("POST", "DELETE"),
        List.of("content-type")
    );
    var kuduLocalLambda = new LocalLambda(
        new LocalLambdaSettings(
            9002,
            8080,
            K * K * 2,
            10,
            kuduCors,
            time
        )
    );

    var kuduClientSettings =
        new LambdaClientSettings(ENV, utcSupplier());
    var handler = KuduLambdaHandler.create(
        kuduClientSettings,
        taninimSettings,
        S3AccessorFactory.defaultFactory(ENV)
    );
    var kuduLambdaManaged = LamdbdaManaged.create(
        kuduLocalLambda.getLambdaUri(),
        kuduClientSettings,
        handler
    );

    logger.info("Kudu: {}", handler);

    var yellinSize = 8 * K;
    var yellinLocalLambda = new LocalLambda(
        new LocalLambdaSettings(
            9001,
            8081,
            yellinSize * 2,
            10,
            yellinCors,
            time
        )
    );

    var yellinClientSettings =
        new LambdaClientSettings(ENV, utcSupplier());

    var yellin = YellinLambdaHandler.handler(
        yellinClientSettings,
        taninimSettings,
        S3AccessorFactory.defaultFactory(ENV),
        FbAuthenticator.simple()
    );
    var yellinLamdbdaManaged = LamdbdaManaged.create(
        yellinLocalLambda.getLambdaUri(),
        yellinClientSettings,
        yellin
    );
    logger.info("Yellin: {}", yellin);

    try (var executor = Executors.newFixedThreadPool(4)) {
        executor.submit(kuduLocalLambda);
        executor.submit(yellinLocalLambda);
        executor.submit(kuduLambdaManaged);
        executor.submit(yellinLamdbdaManaged);
        logger.info("Started");
    }
    logger.info("Stopped");
}

private static final int K = 1_024;

private static final Duration ONE_DAY = Duration.ofDays(1);

private static final Duration FOUR_HOURS = Duration.ofHours(1);

private static final Env ENV = Env.actual();
