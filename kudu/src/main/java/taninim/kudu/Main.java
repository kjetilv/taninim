package taninim.kudu;

import java.time.Duration;

import com.github.kjetilv.uplift.kernel.Env;
import com.github.kjetilv.uplift.kernel.ManagedExecutors;
import com.github.kjetilv.uplift.kernel.Time;
import com.github.kjetilv.uplift.lambda.DefaultLamdbdaManaged;
import com.github.kjetilv.uplift.lambda.LambdaClientSettings;
import com.github.kjetilv.uplift.s3.DefaultS3AccessorFactory;
import com.github.kjetilv.uplift.s3.S3AccessorFactory;
import taninim.TaninimSettings;

public final class Main {

    @SuppressWarnings("MagicNumber")
    public static void main(String[] args) {
        ManagedExecutors.configure(
            10,
            32,
            10
        );
        LambdaClientSettings clientSettings =
            new LambdaClientSettings(ENV, Time.utcSupplier());
        TaninimSettings taninimSettings = new TaninimSettings(
            Duration.ofDays(1),
            Duration.ofHours(4),
            1_024 * 1_024
        );
        S3AccessorFactory s3 = new DefaultS3AccessorFactory(
            ENV,
            ManagedExecutors.executor("S3")
        );
        new DefaultLamdbdaManaged(
            ENV.awsLambdaUri(),
            clientSettings,
            KuduLambdaHandler.create(
                clientSettings,
                taninimSettings,
                s3
            ),
            ManagedExecutors.executor("L")
        ).run();
    }

    private static final Env ENV = Env.actual();
}
