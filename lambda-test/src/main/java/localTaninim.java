import module java.base;
import com.github.kjetilv.uplift.flambda.CorsSettings;
import com.github.kjetilv.uplift.flambda.LocalLambda;
import com.github.kjetilv.uplift.flambda.LocalLambdaSettings;
import com.github.kjetilv.uplift.kernel.Env;
import com.github.kjetilv.uplift.lambda.LambdaClientSettings;
import com.github.kjetilv.uplift.lambda.LamdbdaManaged;
import com.github.kjetilv.uplift.s3.S3AccessorFactory;
import org.slf4j.LoggerFactory;
import taninim.TaninimSettings;
import taninim.fb.DefaultFbAuthenticator;
import taninim.kudu.KuduLambdaHandler;
import taninim.yellin.YellinLambdaHandler;

import static com.github.kjetilv.uplift.flogs.Flogs.initialize;
import static com.github.kjetilv.uplift.flogs.LogLevel.DEBUG;
import static com.github.kjetilv.uplift.util.Time.utcSupplier;

void main() {
    initialize(DEBUG);

    var logger = LoggerFactory.getLogger("LocalTaninim");
    var time = utcSupplier();

    var taninimSettings = new TaninimSettings(ONE_DAY, FOUR_HOURS, K * K);

    var kuduCors = new CorsSettings(
        List.of("https://kjetilv.github.io", "https://localhost:5173"),
        List.of("GET"),
        List.of("content-type", "range")
    );
    var yellinCors = new CorsSettings(
        List.of("https://kjetilv.github.io", "https://localhost:5173"),
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
    var kudu = KuduLambdaHandler.create(
        kuduClientSettings,
        taninimSettings,
        S3AccessorFactory.defaultFactory(ENV)
    );
    var kuduLambdaManaged = LamdbdaManaged.create(
        kuduLocalLambda.getLambdaUri(),
        kuduClientSettings,
        kudu
    );

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
        new DefaultFbAuthenticator()
    );
    var yellinLamdbdaManaged = LamdbdaManaged.create(
        yellinLocalLambda.getLambdaUri(),
        yellinClientSettings,
        yellin
    );

    try (var executor = Executors.newFixedThreadPool(4)) {
        List.of(
                kuduLocalLambda,
                yellinLocalLambda,
                kuduLambdaManaged,
                yellinLamdbdaManaged
            )
            .forEach(executor::submit);
        logger.info("Yellin @ {}: {}", yellinLamdbdaManaged.lambdaUri(), yellin);
        logger.info("Kudu   @ {}: {}", kuduLambdaManaged.lambdaUri(), kudu);
    }
    logger.info("Stopped");
}

private static final int K = 1_024;

private static final Duration ONE_DAY = Duration.ofDays(1);

private static final Duration FOUR_HOURS = Duration.ofHours(1);

private static final Env ENV = Env.actual();
