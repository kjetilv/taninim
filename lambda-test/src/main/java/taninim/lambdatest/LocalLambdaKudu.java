package taninim.lambdatest;

import com.github.kjetilv.uplift.flambda.CorsSettings;
import com.github.kjetilv.uplift.flambda.LocalLambda;
import com.github.kjetilv.uplift.flambda.LocalLambdaSettings;
import com.github.kjetilv.uplift.kernel.Env;
import com.github.kjetilv.uplift.kernel.ManagedExecutors;
import com.github.kjetilv.uplift.kernel.Time;
import com.github.kjetilv.uplift.lambda.LambdaClientSettings;
import com.github.kjetilv.uplift.lambda.LambdaHandler;
import com.github.kjetilv.uplift.lambda.LamdbdaManaged;
import com.github.kjetilv.uplift.s3.DefaultS3AccessorFactory;
import taninim.TaninimSettings;
import taninim.kudu.KuduLambdaHandler;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;

@SuppressWarnings({ "MagicNumber", "resource" })
public final class LocalLambdaKudu {

    public static void main(String[] args) {
        ManagedExecutors.configure(
            10,
            32,
            10
        );

        CorsSettings corsSettings = new CorsSettings(
            List.of("https://tanin.im:5173"),
            List.of("GET"),
            List.of("content-type", "range")
        );

        LocalLambdaSettings settings = new LocalLambdaSettings(
            9002,
            8080,
            8 * 8192,
            10,
            corsSettings,
            Time.utcSupplier()
        );

        LocalLambda localLambda = new LocalLambda(
            settings,
            ManagedExecutors.executor("aws-L"),
            ManagedExecutors.executor("aws-S")
        );

        LambdaClientSettings clientSettings =
            new LambdaClientSettings(Env.actual(), Time.utcSupplier());

        TaninimSettings taninimSettings = new TaninimSettings(
            Duration.ofDays(1),
            Duration.ofHours(4),
            1024 * 1024
        );

        LambdaHandler handler = KuduLambdaHandler.create(
            clientSettings,
            taninimSettings,
            new DefaultS3AccessorFactory(Env.actual(), ManagedExecutors.executor("S3"))
        );

        Runnable lamdbdaManaged = LamdbdaManaged.create(
            localLambda.getLambdaUri(),
            clientSettings,
            handler,
            ManagedExecutors.executor("L")
        );

        ExecutorService executor = ManagedExecutors.executor("runner");

        executor.submit(localLambda);
        executor.submit(lamdbdaManaged);
        executor.shutdown();
    }
}
