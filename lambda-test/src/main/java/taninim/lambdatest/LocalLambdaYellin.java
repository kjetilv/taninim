package taninim.lambdatest;

import com.github.kjetilv.uplift.flambda.CorsSettings;
import com.github.kjetilv.uplift.flambda.LocalLambda;
import com.github.kjetilv.uplift.flambda.LocalLambdaSettings;
import com.github.kjetilv.uplift.flogs.Flogs;
import com.github.kjetilv.uplift.flogs.LogLevel;
import com.github.kjetilv.uplift.kernel.Env;
import com.github.kjetilv.uplift.kernel.ManagedExecutors;
import com.github.kjetilv.uplift.kernel.Time;
import com.github.kjetilv.uplift.lambda.LambdaClientSettings;
import com.github.kjetilv.uplift.lambda.LambdaHandler;
import com.github.kjetilv.uplift.lambda.LamdbdaManaged;
import com.github.kjetilv.uplift.s3.DefaultS3AccessorFactory;
import taninim.TaninimSettings;
import taninim.fb.FbAuthenticator;
import taninim.yellin.YellinLambdaHandler;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static com.github.kjetilv.uplift.kernel.ManagedExecutors.executor;

@SuppressWarnings({ "MagicNumber" })
public final class LocalLambdaYellin {

    public static void main(String[] args) {
        ManagedExecutors.configure(
            10,
            32,
            10
        );
        Flogs.initialize(LogLevel.DEBUG, executor("logging"));

        LocalLambdaSettings settings = new LocalLambdaSettings(
            9001,
            8081,
            8 * 8192,
            10,
            new CorsSettings(
                List.of("https://tanin.im:5173"),
                List.of("POST", "DELETE"),
                List.of("content-type")
            ),
            Time.utcSupplier()
        );

        LocalLambda localLambda = new LocalLambda(
            settings,
            executor("aws-L"),
            executor("aws-S")
        );

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
            new DefaultS3AccessorFactory(Env.actual(), executor("S3")),
            new FbAuthenticator()
        );

        Runnable lamdbdaManaged = LamdbdaManaged.create(
            localLambda.getLambdaUri(),
            clientSettings,
            yellin,
            executor("L")
        );

        try (ExecutorService executor = executor("runner", 2)) {
            executor.submit(localLambda);
            executor.submit(lamdbdaManaged);
            executor.shutdown();
        }
    }
}
