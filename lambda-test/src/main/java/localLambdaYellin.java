import module java.base;
import com.github.kjetilv.uplift.synchttp.CorsSettings;
import com.github.kjetilv.uplift.flambda.Flambda;
import com.github.kjetilv.uplift.flambda.FlambdaSettings;
import com.github.kjetilv.uplift.flogs.LogLevel;
import com.github.kjetilv.uplift.kernel.Env;
import com.github.kjetilv.uplift.lambda.Lambda;
import com.github.kjetilv.uplift.lambda.LambdaClientSettings;
import com.github.kjetilv.uplift.s3.S3AccessorFactory;
import com.github.kjetilv.uplift.util.Time;
import org.slf4j.LoggerFactory;
import taninim.TaninimSettings;
import taninim.fb.DefaultFbAuthenticator;
import taninim.yellin.DefaultYellin;
import taninim.yellin.YellinLambdaHandler;

import static com.github.kjetilv.uplift.flogs.Flogs.initialize;

@SuppressWarnings({"MagicNumber"})
void main() {
    initialize(LogLevel.DEBUG);

    var logger = LoggerFactory.getLogger("LocalLambdaYellin");

    var settings = new FlambdaSettings(
        "yellin",
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

    var flambda = new Flambda(settings);

    var clientSettings =
        new LambdaClientSettings(Env.actual(), Time.utcSupplier());

    var taninimSettings = new TaninimSettings(
        Duration.ofDays(1),
        Duration.ofHours(1),
        1024 * 1024
    );

    var authenticator = new DefaultFbAuthenticator();

    var yellin = new YellinLambdaHandler(DefaultYellin.create(
        S3AccessorFactory.defaultFactory(Env.actual()).create(),
        clientSettings.time(),
        taninimSettings.sessionDuration(),
        taninimSettings.leaseDuration(),
        authenticator
    ));

    var lamdbdaManaged = Lambda.managed(
        flambda.lambdaUri(),
        clientSettings,
        yellin
    );
    try (var executor = Executors.newFixedThreadPool(2)) {
        executor.submit(() ->
            lamdbdaManaged.accept("yellin"));
        logger.info("Started");
    }
}
