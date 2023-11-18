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

@SuppressWarnings({ "MagicNumber" })
public final class LocalTaninim {

    public static void main(String[] args) {
        ManagedExecutors.configure(
            4,
            10,
            10
        );
        Flogs.initialize(DEBUG, ManagedExecutors.executor("logger", 1));

        Env env = Env.actual();
        Supplier<Instant> time = Time.utcSupplier();

        int kuduSize = 1_024 * 1_024;

        TaninimSettings taninimSettings = new TaninimSettings(
            Duration.ofDays(1),
            Duration.ofHours(1),
            kuduSize
        );
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
                kuduSize * 2,
                10,
                kuduCors,
                time
            ),
            ManagedExecutors.executor("lambda-server-kudu"),
            ManagedExecutors.executor("api-server-kudu")
        );
        LambdaClientSettings kuduClientSettings =
            new LambdaClientSettings(env, Time.utcSupplier());
        LambdaHandler handler = KuduLambdaHandler.create(
            kuduClientSettings,
            taninimSettings,
            new DefaultS3AccessorFactory(env, ManagedExecutors.executor("kudu-s3"))
        );
        Runnable kuduLambdaManaged = LamdbdaManaged.create(
            kuduLocalLambda.getLambdaUri(),
            kuduClientSettings,
            handler,
            ManagedExecutors.executor("kudu-lambda")
        );

        logger().info("Kudu: {}", handler);

        int yellinSize = 8 * 1_024;
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
            new LambdaClientSettings(env, Time.utcSupplier());

        LambdaHandler yellin = YellinLambdaHandler.handler(
            yellinClientSettings,
            taninimSettings,
            new DefaultS3AccessorFactory(env, ManagedExecutors.executor("yellin-s3")),
            new FbAuthenticator(Json.STRING_2_JSON_MAP)
        );
        Runnable yellinLamdbdaManaged = LamdbdaManaged.create(
            yellinLocalLambda.getLambdaUri(),
            yellinClientSettings,
            yellin,
            ManagedExecutors.executor("yellin-lambda")
        );
        logger().info("Yellin: {}", yellin);

        ExecutorService runner = ManagedExecutors.executor("runner");

        runner.submit(kuduLocalLambda);
        runner.submit(yellinLocalLambda);
        runner.submit(kuduLambdaManaged);
        runner.submit(yellinLamdbdaManaged);

        logger().info("Started");
    }

    private static Logger logger() {
        return LoggerFactory.getLogger(LocalTaninim.class);
    }
}
