import module java.base;
import com.github.kjetilv.uplift.flambda.Flambda;
import com.github.kjetilv.uplift.flambda.FlambdaSettings;
import com.github.kjetilv.uplift.flogs.BriefLogEntryFormatter;
import com.github.kjetilv.uplift.flogs.Flogs;
import com.github.kjetilv.uplift.kernel.Env;
import com.github.kjetilv.uplift.lambda.Lambda;
import com.github.kjetilv.uplift.lambda.LambdaClientSettings;
import com.github.kjetilv.uplift.s3.S3AccessorFactory;
import com.github.kjetilv.uplift.synchttp.CorsSettings;
import org.slf4j.LoggerFactory;
import taninim.TaninimSettings;
import taninim.fb.DefaultFbAuthenticator;
import taninim.kudu.DefaultKudu;
import taninim.kudu.KuduLambdaHandler;
import taninim.yellin.DefaultYellin;
import taninim.yellin.YellinLambdaHandler;

import static com.github.kjetilv.uplift.flogs.LogLevel.INFO;
import static com.github.kjetilv.uplift.util.Time.utcSupplier;

void main() {
    Flogs.initialize(INFO, new BriefLogEntryFormatter());

    var logger = LoggerFactory.getLogger("LocalTaninim");
    var time = utcSupplier();

    var taninimSettings = new TaninimSettings(ONE_DAY, FOUR_HOURS, K * K);

    var kuduCors = new CorsSettings(
        List.of(
            "https://kjetilv.github.io",
            "https://localhost:8443"
        ),
        List.of("GET", "OPTIONS", "HEAD"),
        List.of("content-type", "range")
    );
    var kuduFlambda = new Flambda(
        new FlambdaSettings(
            "kudu",
            9002,
            8082,
            K * K,
            10,
            kuduCors,
            time
        ));

    var kuduClientSettings = new LambdaClientSettings(ENV, utcSupplier());
    var kudu = new KuduLambdaHandler(DefaultKudu.create(
        kuduClientSettings,
        taninimSettings,
        S3AccessorFactory.defaultFactory(ENV)
    ));
    var kuduLambdaManaged = Lambda.managed(
        kuduFlambda.lambdaUri(),
        kuduClientSettings,
        kudu
    );

    var yellinCors = new CorsSettings(
        List.of(
//            "https://kjetilv.github.io",
            "https://localhost:8443"
//            "https://localhost:5173"
        ),
        List.of("POST", "DELETE", "OPTIONS", "HEAD"),
        List.of("content-type")
    );
    var yellinSize = 8 * K;
    var yellinFlambda = new Flambda(
        new FlambdaSettings(
            "yellin",
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

    var authenticator = new DefaultFbAuthenticator();

    var yellin = DefaultYellin.create(
        S3AccessorFactory.defaultFactory(ENV).create(),
        yellinClientSettings.time(),
        taninimSettings.sessionDuration(),
        taninimSettings.leaseDuration(),
        authenticator
    );

    var yellinHandler = new YellinLambdaHandler(yellin);

    var yellinLamdbdaManaged = Lambda.managed(
        yellinFlambda.lambdaUri(),
        yellinClientSettings,
        yellinHandler
    );

    try (var executor = Executors.newFixedThreadPool(4)) {
        List.of(
                kuduLambdaManaged.looper("kudu"),
                yellinLamdbdaManaged.looper("yellin"),
                kuduFlambda,
                yellinFlambda
            )
            .forEach(executor::submit);
        logger.info(
            "Yellin ➤ {} ⇌ 𝛌⏐{}: {}",
            yellinFlambda.apiUri(),
            yellinFlambda.lambdaUri(),
            yellinHandler
        );
        logger.info(
            "Kudu   ➤ {} ⇌ 𝛌⏐/{}: {}",
            kuduFlambda.apiUri(),
            kuduFlambda.lambdaUri(),
            kudu
        );
    }
    logger.info("Stopped");
}

private static final int K = 1_024;

private static final Duration ONE_DAY = Duration.ofDays(1);

private static final Duration FOUR_HOURS = Duration.ofHours(4);

private static final Env ENV = Env.actual();
