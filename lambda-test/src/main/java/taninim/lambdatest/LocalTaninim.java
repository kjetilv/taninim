package taninim.lambdatest;

import com.github.kjetilv.uplift.flambda.CorsSettings;
import com.github.kjetilv.uplift.flambda.LocalLambda;
import com.github.kjetilv.uplift.flambda.LocalLambdaSettings;
import com.github.kjetilv.uplift.flogs.Flogs;
import com.github.kjetilv.uplift.kernel.Env;
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
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import static com.github.kjetilv.uplift.flogs.LogLevel.DEBUG;

public final class LocalTaninim {

    public static void main(String[] args) {
        Flogs.initialize(DEBUG);

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
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        LocalLambda kuduLocalLambda = new LocalLambda(
            new LocalLambdaSettings(
                9002,
                8080,
                K * K * 2,
                10,
                kuduCors,
                time
            ),
            executor,
            executor
        );

        LambdaClientSettings kuduClientSettings =
            new LambdaClientSettings(Env.actual(), Time.utcSupplier());
        LambdaHandler handler = KuduLambdaHandler.create(
            kuduClientSettings,
            taninimSettings,
            new DefaultS3AccessorFactory(
                Env.actual(),
                executor
            )
        );
        Runnable kuduLambdaManaged = LamdbdaManaged.create(
            kuduLocalLambda.getLambdaUri(),
            kuduClientSettings,
            handler,
            executor
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
            executor,
            executor
        );

        LambdaClientSettings yellinClientSettings =
            new LambdaClientSettings(Env.actual(), Time.utcSupplier());

        LambdaHandler yellin = YellinLambdaHandler.handler(
            yellinClientSettings,
            taninimSettings,
            new DefaultS3AccessorFactory(
                Env.actual(),
                executor
            ),
            new FbAuthenticator()
        );
        Runnable yellinLamdbdaManaged = LamdbdaManaged.create(
            yellinLocalLambda.getLambdaUri(),
            yellinClientSettings,
            yellin,
            executor
        );
        logger().info("Yellin: {}", yellin);

        executor.submit(kuduLocalLambda);
        executor.submit(yellinLocalLambda);
        executor.submit(kuduLambdaManaged);
        executor.submit(yellinLamdbdaManaged);
        logger().info("Started");
        logger().info("Stopped");
    }

    private static final int K = 1_024;

    private static final Duration ONE_DAY = Duration.ofDays(1);

    private static final Duration FOUR_HOURS = Duration.ofHours(1);

    private static Logger logger() {
        return LoggerFactory.getLogger(LocalTaninim.class);
    }
}
