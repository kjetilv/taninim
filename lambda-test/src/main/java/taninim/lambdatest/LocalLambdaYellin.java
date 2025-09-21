package taninim.lambdatest;

import module java.base;
import module taninim.fb;
import module taninim.taninim;
import module taninim.yellin;
import module uplift.flambda;
import module uplift.flogs;
import module uplift.kernel;
import module uplift.lambda;
import module uplift.s3;
import module uplift.util;
import org.slf4j.Logger;

@SuppressWarnings({"MagicNumber"})
public final class LocalLambdaYellin {

    static void main() {
        Flogs.initialize(LogLevel.DEBUG);

        Logger logger = LoggerFactory.getLogger(LocalLambdaYellin.class);

        LocalLambdaSettings settings = new LocalLambdaSettings(
            9001,
            8081,
            8 * 8192,
            10,
            new CorsSettings(
                List.of("https://kjetilv.github.io"),
                List.of("POST", "DELETE"),
                List.of("content-type")
            ),
            Time.utcSupplier()
        );

        LocalLambda localLambda = new LocalLambda(settings );

        LambdaClientSettings clientSettings =
            new LambdaClientSettings(Env.actual(), Time.utcSupplier());

        TaninimSettings taninimSettings = new TaninimSettings(
            Duration.ofDays(1),
            Duration.ofHours(1),
            1024 * 1024
        );

        LambdaHandler yellin = YellinLambdaHandler.handler(
            clientSettings,
            taninimSettings,
            S3AccessorFactory.defaultFactory(Env.actual()),
            FbAuthenticator.simple()
        );

        Runnable lamdbdaManaged = LamdbdaManaged.create(
            localLambda.getLambdaUri(),
            clientSettings,
            yellin
        );
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            executor.submit(localLambda);
            executor.submit(lamdbdaManaged);
            logger.info("Started");
        }
    }

    private LocalLambdaYellin() {
    }
}
