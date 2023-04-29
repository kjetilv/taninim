package mediaserver.lambdatest;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;

import com.github.kjetilv.uplift.flambda.CorsSettings;
import com.github.kjetilv.uplift.flambda.LocalLambda;
import com.github.kjetilv.uplift.flambda.LocalLambdaSettings;
import com.github.kjetilv.uplift.kernel.Env;
import com.github.kjetilv.uplift.kernel.Time;
import com.github.kjetilv.uplift.lambda.LambdaClientSettings;
import com.github.kjetilv.uplift.lambda.LambdaHandler;
import com.github.kjetilv.uplift.lambda.LamdbdaManaged;
import com.github.kjetilv.uplift.s3.DefaultS3AccessorFactory;
import mediaserver.kudu.KuduLambdaHandler;
import mediaserver.taninim.TaninimSettings;

import static com.github.kjetilv.uplift.kernel.ManagedExecutors.executor;

@SuppressWarnings({ "MagicNumber", "resource", "IOResourceOpenedButNotSafelyClosed" })
public final class LocalLambdaKudu {

    public static void main(String[] args) {
        LocalLambda localLambda = new LocalLambda(
            new LocalLambdaSettings(
                9002,
                8080,
                8 * 8192,
                10,
                executor("aws-L", 10),
                executor("aws-S", 10),
                new CorsSettings(
                    List.of("https://tanin.im:5173"),
                    List.of("GET"),
                    List.of("content-type", "range")
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
        TaninimSettings taninimSettings = new TaninimSettings(
            Duration.ofDays(1),
            Duration.ofHours(4),
            1024 * 1024
        );
        LambdaHandler handler = KuduLambdaHandler.create(
            clientSettings,
            taninimSettings,
            new DefaultS3AccessorFactory(env, executor("S3", 10))
        );

        LamdbdaManaged lamdbdaManaged =
            new LamdbdaManaged(localLambda.getLambdaUri(), clientSettings, handler);

        ExecutorService executor = executor("runner", 2);

        executor.submit(localLambda);
        executor.submit(lamdbdaManaged);
        executor.shutdown();
    }
}
