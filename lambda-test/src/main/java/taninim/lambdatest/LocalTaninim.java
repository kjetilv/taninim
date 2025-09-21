package taninim.lambdatest;

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
import org.slf4j.Logger;

import static com.github.kjetilv.uplift.flogs.LogLevel.DEBUG;

public final class LocalTaninim {

    static void main() {
        Flogs.initialize(DEBUG);
        Logger logger = LoggerFactory.getLogger(LocalTaninim.class);

        Supplier<Instant> time = Time.utcSupplier();

        TaninimSettings taninimSettings = new TaninimSettings(ONE_DAY, FOUR_HOURS, K * K);

        CorsSettings kuduCors = new CorsSettings(
            List.of("https://kjetilv.github.io"),
            List.of("GET"),
            List.of("content-type", "range")
        );
        CorsSettings yellinCors = new CorsSettings(
            List.of("https://kjetilv.github.io"),
            List.of("POST", "DELETE"),
            List.of("content-type")
        );
        LocalLambda kuduLocalLambda = new LocalLambda(
            new LocalLambdaSettings(
                9002,
                8080,
                K * K * 2,
                10,
                kuduCors,
                time
            )
        );

        LambdaClientSettings kuduClientSettings =
            new LambdaClientSettings(ENV, com.github.kjetilv.uplift.util.Time.utcSupplier());
        LambdaHandler handler = KuduLambdaHandler.create(
            kuduClientSettings,
            taninimSettings,
            S3AccessorFactory.defaultFactory(ENV)
        );
        Runnable kuduLambdaManaged = LamdbdaManaged.create(
            kuduLocalLambda.getLambdaUri(),
            kuduClientSettings,
            handler
        );

        logger.info("Kudu: {}", handler);

        int yellinSize = 8 * K;
        LocalLambda yellinLocalLambda = new LocalLambda(
            new LocalLambdaSettings(
                9001,
                8081,
                yellinSize * 2,
                10,
                yellinCors,
                time
            )
        );

        LambdaClientSettings yellinClientSettings =
            new LambdaClientSettings(ENV, Time.utcSupplier());

        LambdaHandler yellin = YellinLambdaHandler.handler(
            yellinClientSettings,
            taninimSettings,
            S3AccessorFactory.defaultFactory(ENV),
            FbAuthenticator.simple()
        );
        Runnable yellinLamdbdaManaged = LamdbdaManaged.create(
            yellinLocalLambda.getLambdaUri(),
            yellinClientSettings,
            yellin
        );
        logger.info("Yellin: {}", yellin);

        try (ExecutorService executor = Executors.newFixedThreadPool(4)) {
            executor.submit(kuduLocalLambda);
            executor.submit(yellinLocalLambda);
            executor.submit(kuduLambdaManaged);
            executor.submit(yellinLamdbdaManaged);
            logger.info("Started");
        }
        logger.info("Stopped");
    }

    private LocalTaninim() {
    }

    private static final int K = 1_024;

    private static final Duration ONE_DAY = Duration.ofDays(1);

    private static final Duration FOUR_HOURS = Duration.ofHours(1);

    private static final Env ENV = Env.actual();
}
