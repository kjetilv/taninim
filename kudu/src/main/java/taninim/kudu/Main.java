package taninim.kudu;

import java.time.Duration;

import com.github.kjetilv.uplift.kernel.Env;
import com.github.kjetilv.uplift.kernel.Time;
import com.github.kjetilv.uplift.lambda.DefaultLamdbdaManaged;
import com.github.kjetilv.uplift.lambda.LambdaClientSettings;
import com.github.kjetilv.uplift.s3.DefaultS3AccessorFactory;
import com.github.kjetilv.uplift.s3.S3AccessorFactory;
import taninim.taninim.TaninimSettings;

import static com.github.kjetilv.uplift.kernel.ManagedExecutors.executor;

public final class Main {

    @SuppressWarnings("MagicNumber")
    public static void main(String[] args) {
        LambdaClientSettings clientSettings = new LambdaClientSettings(
            ENV,
            executor("L", 10),
            executor("S", 10),
            Time.utcSupplier()
        );
        TaninimSettings taninimSettings = new TaninimSettings(
            Duration.ofDays(1),
            Duration.ofHours(4),
            1_024 * 1_024
        );
        S3AccessorFactory s3 = new DefaultS3AccessorFactory(
            ENV,
            executor("S3", 10)
        );
        new DefaultLamdbdaManaged(
            ENV.awsLambdaUri(),
            clientSettings,
            KuduLambdaHandler.create(
                clientSettings,
                taninimSettings,
                s3
            )
        ).run();
    }

    private static final Env ENV = Env.actual();
}
