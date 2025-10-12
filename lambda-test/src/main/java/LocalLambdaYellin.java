import module java.base;
import module taninim.fb;
import module taninim.taninim;
import module taninim.yellin;
import module uplift.flambda;
import module uplift.kernel;
import module uplift.lambda;
import module uplift.s3;
import module uplift.util;
import com.github.kjetilv.uplift.flogs.LogLevel;
import org.slf4j.LoggerFactory;

import static com.github.kjetilv.uplift.flogs.Flogs.initialize;

@SuppressWarnings({"MagicNumber"})
void main() {
    initialize(LogLevel.DEBUG);

    var logger = LoggerFactory.getLogger("LocalLambdaYellin");

    var settings = new LocalLambdaSettings(
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

    var localLambda = new LocalLambda(settings);

    var clientSettings =
        new LambdaClientSettings(Env.actual(), Time.utcSupplier());

    var taninimSettings = new TaninimSettings(
        Duration.ofDays(1),
        Duration.ofHours(1),
        1024 * 1024
    );

    var yellin = YellinLambdaHandler.handler(
        clientSettings,
        taninimSettings,
        S3AccessorFactory.defaultFactory(Env.actual()),
        FbAuthenticator.simple()
    );

    var lamdbdaManaged = LamdbdaManaged.create(
        localLambda.getLambdaUri(),
        clientSettings,
        yellin
    );
    try (var executor = Executors.newFixedThreadPool(2)) {
        executor.submit(localLambda);
        executor.submit(lamdbdaManaged);
        logger.info("Started");
    }
}
