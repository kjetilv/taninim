import module java.base;
import com.github.kjetilv.uplift.flambda.CorsSettings;
import com.github.kjetilv.uplift.flambda.LocalLambda;
import com.github.kjetilv.uplift.flambda.LocalLambdaSettings;
import com.github.kjetilv.uplift.flogs.LogLevel;
import com.github.kjetilv.uplift.kernel.Env;
import com.github.kjetilv.uplift.lambda.LambdaClientSettings;
import com.github.kjetilv.uplift.lambda.LamdbdaManaged;
import com.github.kjetilv.uplift.s3.S3AccessorFactory;
import com.github.kjetilv.uplift.util.Time;
import org.slf4j.LoggerFactory;
import taninim.TaninimSettings;
import taninim.fb.FbAuthenticator;
import taninim.yellin.YellinLambdaHandler;

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
