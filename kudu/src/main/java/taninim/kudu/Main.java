package taninim.kudu;

import com.github.kjetilv.uplift.flogs.Flogs;
import com.github.kjetilv.uplift.kernel.Env;
import com.github.kjetilv.uplift.kernel.ManagedExecutors;
import com.github.kjetilv.uplift.kernel.Time;
import com.github.kjetilv.uplift.lambda.LambdaClientSettings;
import com.github.kjetilv.uplift.lambda.LambdaHandler;
import com.github.kjetilv.uplift.lambda.LamdbdaManaged;
import com.github.kjetilv.uplift.s3.DefaultS3AccessorFactory;
import com.github.kjetilv.uplift.s3.S3AccessorFactory;
import taninim.TaninimSettings;

import java.time.Duration;
import java.util.concurrent.ExecutorService;

import static com.github.kjetilv.uplift.flogs.LogLevel.DEBUG;

public final class Main {

    @SuppressWarnings("MagicNumber")
    public static void main(String[] args) {
        ManagedExecutors.configure(10, 32, 10);
        Flogs.initialize(DEBUG, ManagedExecutors.executor("logger", 1));
        try {
            LambdaClientSettings clientSettings =
                new LambdaClientSettings(ENV, Time.utcSupplier());

            TaninimSettings taninimSettings =
                new TaninimSettings(A_DAY, FOUR_HOURS, K * K);

            S3AccessorFactory s3 =
                new DefaultS3AccessorFactory(ENV, ManagedExecutors.executor("S3"));

            LambdaHandler lambdaHandler = KuduLambdaHandler.create(clientSettings, taninimSettings, s3);
            ExecutorService lambdaExecutor = ManagedExecutors.executor("L");

            LamdbdaManaged.create(ENV.awsLambdaUri(), clientSettings, lambdaHandler, lambdaExecutor).run();
        } finally {
            Flogs.close();
        }
    }

    public static final Duration A_DAY = Duration.ofDays(1);

    public static final Duration FOUR_HOURS = Duration.ofHours(4);

    private static final Env ENV = Env.actual();

    private static final int K = 1_024;
}
