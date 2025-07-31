package taninim.kudu;

import com.github.kjetilv.uplift.flogs.Flogs;
import com.github.kjetilv.uplift.kernel.Env;
import com.github.kjetilv.uplift.kernel.Time;
import com.github.kjetilv.uplift.lambda.LambdaClientSettings;
import com.github.kjetilv.uplift.lambda.LambdaHandler;
import com.github.kjetilv.uplift.lambda.LamdbdaManaged;
import com.github.kjetilv.uplift.s3.DefaultS3AccessorFactory;
import com.github.kjetilv.uplift.s3.S3AccessorFactory;
import taninim.TaninimSettings;

import java.time.Duration;
import java.util.concurrent.Executors;

import static com.github.kjetilv.uplift.flogs.LogLevel.DEBUG;

public final class Main {

    @SuppressWarnings("MagicNumber")
    public static void main(String[] args) {
        Flogs.initialize(DEBUG);

        LambdaClientSettings clientSettings =
            new LambdaClientSettings(ENV, Time.utcSupplier());

        TaninimSettings taninimSettings =
            new TaninimSettings(A_DAY, FOUR_HOURS, K * K);

        S3AccessorFactory s3 =
            new DefaultS3AccessorFactory(ENV, Executors.newVirtualThreadPerTaskExecutor());

        LambdaHandler lambdaHandler = KuduLambdaHandler.create(clientSettings, taninimSettings, s3);
        LamdbdaManaged.create(
            ENV.awsLambdaUri(),
            clientSettings,
            lambdaHandler,
            Executors.newVirtualThreadPerTaskExecutor()
        ).run();
    }

    private static final Duration A_DAY = Duration.ofDays(1);

    private static final Duration FOUR_HOURS = Duration.ofHours(4);

    private static final Env ENV = Env.actual();

    private static final int K = 1_024;
}
