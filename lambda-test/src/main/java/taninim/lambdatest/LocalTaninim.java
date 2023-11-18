package taninim.lambdatest;

import com.github.kjetilv.uplift.flambda.CorsSettings;
import com.github.kjetilv.uplift.flambda.LocalLambda;
import com.github.kjetilv.uplift.flambda.LocalLambdaSettings;
import com.github.kjetilv.uplift.flogs.Flogs;
import com.github.kjetilv.uplift.json.Json;
import com.github.kjetilv.uplift.kernel.Env;
import com.github.kjetilv.uplift.kernel.ManagedExecutors;
import com.github.kjetilv.uplift.kernel.Time;
import com.github.kjetilv.uplift.lambda.LambdaClientSettings;
import com.github.kjetilv.uplift.lambda.LambdaHandler;
import com.github.kjetilv.uplift.lambda.LamdbdaManaged;
import com.github.kjetilv.uplift.s3.DefaultS3AccessorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taninim.TaninimSettings;
import taninim.fb.FbAuthenticator;
import taninim.kudu.KuduLambdaHandler;
import taninim.yellin.YellinLambdaHandler;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import static com.github.kjetilv.uplift.flogs.LogLevel.DEBUG;

public final class LocalTaninim {

    public static void main(String[] args) {
        Flogs.initialize(DEBUG, ManagedExecutors.executor("logger", 1));

        Supplier<Instant> time = Time.utcSupplier();

        TaninimSettings taninimSettings = new TaninimSettings(ONE_DAY, FOUR_HOURS, K * K);

        CorsSettings kuduCors = new CorsSettings(
            List.of("https://tanin.im:5173", "https://kjetilv.github.io"),
            List.of("GET"),
            List.of("content-type", "range")
        );
        CorsSettings yellinCors = new CorsSettings(
            List.of("https://tanin.im:5173", "https://kjetilv.github.io"),
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
            ),
            ManagedExecutors.executor("lambda-server-kudu"),
            ManagedExecutors.executor("api-server-kudu")
        );

        LambdaClientSettings kuduClientSettings =
            new LambdaClientSettings(Env.actual(), Time.utcSupplier());
        LambdaHandler handler = KuduLambdaHandler.create(
            kuduClientSettings,
            taninimSettings,
            new DefaultS3AccessorFactory(
                Env.actual(),
                ManagedExecutors.executor("kudu-s3")
            )
        );
        Runnable kuduLambdaManaged = LamdbdaManaged.create(
            kuduLocalLambda.getLambdaUri(),
            kuduClientSettings,
            handler,
            ManagedExecutors.executor("kudu-lambda")
        );

        logger().info("Kudu: {}", handler);

        int yellinSize = 8 * K;
        LocalLambda yellinLocalLambda = new LocalLambda(
            new LocalLambdaSettings(
                9001,
                8081,
                yellinSize * 2,
                10,
                yellinCors,
                time
            ),
            ManagedExecutors.executor("lambda-server-yellin"),
            ManagedExecutors.executor("api-server-yellin")
        );

        LambdaClientSettings yellinClientSettings =
            new LambdaClientSettings(Env.actual(), Time.utcSupplier());

        LambdaHandler yellin = YellinLambdaHandler.handler(
            yellinClientSettings,
            taninimSettings,
            new DefaultS3AccessorFactory(Env.actual(), ManagedExecutors.executor("yellin-s3")),
            new FbAuthenticator(Json.STRING_2_JSON_MAP)
        );
        Runnable yellinLamdbdaManaged = LamdbdaManaged.create(
            yellinLocalLambda.getLambdaUri(),
            yellinClientSettings,
            yellin,
            ManagedExecutors.executor("yellin-lambda")
        );
        logger().info("Yellin: {}", yellin);

        try (ExecutorService runner = ManagedExecutors.executor("runner")) {
            runner.submit(kuduLocalLambda);
            runner.submit(yellinLocalLambda);
            runner.submit(kuduLambdaManaged);
            runner.submit(yellinLamdbdaManaged);
            logger().info("Started");
        }
        logger().info("Stopped");
    }

    public static final int K = 1_024;

    public static final Duration ONE_DAY = Duration.ofDays(1);

    public static final Duration FOUR_HOURS = Duration.ofHours(1);

    private static Logger logger() {
        return LoggerFactory.getLogger(LocalTaninim.class);
    }
}
