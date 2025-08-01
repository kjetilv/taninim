package taninim.lambdatest;

import com.github.kjetilv.uplift.flambda.CorsSettings;
import com.github.kjetilv.uplift.flambda.LocalLambda;
import com.github.kjetilv.uplift.flambda.LocalLambdaSettings;
import com.github.kjetilv.uplift.flogs.Flogs;
import com.github.kjetilv.uplift.flogs.LogLevel;
import com.github.kjetilv.uplift.kernel.Env;
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
import java.util.concurrent.Executors;

@SuppressWarnings({"MagicNumber"})
public final class LocalLambdaYellin {

    public static void main(String[] args) {
        Flogs.initialize(LogLevel.DEBUG);

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

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        LocalLambda localLambda = new LocalLambda(
            settings,
            executor,
            executor
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
            new DefaultS3AccessorFactory(Env.actual(), executor),
            new FbAuthenticator()
        );

        Runnable lamdbdaManaged = LamdbdaManaged.create(
            localLambda.getLambdaUri(),
            clientSettings,
            yellin,
            executor
        );

        executor.submit(localLambda);
        executor.submit(lamdbdaManaged);
    }
}
