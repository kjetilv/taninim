package mediaserver.lambdatest;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;

import com.github.kjetilv.uplift.flambda.CorsSettings;
import com.github.kjetilv.uplift.flambda.LocalLambda;
import com.github.kjetilv.uplift.flambda.LocalLambdaSettings;
import com.github.kjetilv.uplift.flogs.Flogs;
import com.github.kjetilv.uplift.json.Json;
import com.github.kjetilv.uplift.kernel.Env;
import com.github.kjetilv.uplift.kernel.ManagedExecutors;
import com.github.kjetilv.uplift.kernel.Time;
import com.github.kjetilv.uplift.lambda.DefaultLamdbdaManaged;
import com.github.kjetilv.uplift.lambda.LambdaClientSettings;
import com.github.kjetilv.uplift.lambda.LambdaHandler;
import com.github.kjetilv.uplift.lambda.LamdbdaManaged;
import com.github.kjetilv.uplift.s3.DefaultS3AccessorFactory;
import mediaserver.fb.FbAuthenticator;
import mediaserver.taninim.TaninimSettings;
import mediaserver.yellin.YellinLambdaHandler;

import static com.github.kjetilv.uplift.kernel.ManagedExecutors.executor;

@SuppressWarnings({ "MagicNumber", "IOResourceOpenedButNotSafelyClosed", "resource" })
public final class LocalLambdaYellin {

    public static void main(String[] args) {
        Flogs.initialize(ManagedExecutors.threadNamer());

        LocalLambda localLambda = new LocalLambda(
            new LocalLambdaSettings(
                9001,
                8081,
                8 * 8192,
                10,
                executor("aws-L", 10),
                executor("aws-S", 10),
                new CorsSettings(
                    List.of("https://tanin.im:5173"),
                    List.of("POST", "DELETE"),
                    List.of("content-type")
                ),
                Time.utcSupplier()
            ));
        Env env = Env.actual();
        LambdaClientSettings clientSettings = new LambdaClientSettings(
            env,
            Duration.ofMinutes(1),
            executor("L", 10),
            executor("S", 10),
            Time.utcSupplier()
        );
        LambdaHandler yellin = YellinLambdaHandler.handler(
            clientSettings,
            new TaninimSettings(
                Duration.ofDays(1),
                Duration.ofHours(1),
                1024 * 1024
            ),
            new DefaultS3AccessorFactory(env, executor("S3", 10)),
            new FbAuthenticator(Json.STRING_2_JSON_MAP)
        );

        LamdbdaManaged lamdbdaManaged = new DefaultLamdbdaManaged(
            localLambda.getLambdaUri(),
            clientSettings,
            yellin
        );
        ExecutorService executor = executor("runner", 2);

        executor.submit(localLambda);
        executor.submit(lamdbdaManaged);
        executor.shutdown();
    }
}
