package taninim.yellin;

import com.github.kjetilv.uplift.flogs.Flogs;
import com.github.kjetilv.uplift.flogs.LogLevel;
import com.github.kjetilv.uplift.kernel.Env;
import com.github.kjetilv.uplift.lambda.LambdaClientSettings;
import com.github.kjetilv.uplift.lambda.LambdaHandler;
import com.github.kjetilv.uplift.lambda.LamdbdaManaged;
import com.github.kjetilv.uplift.s3.DefaultS3AccessorFactory;
import com.github.kjetilv.uplift.s3.S3AccessorFactory;
import taninim.TaninimSettings;
import taninim.fb.Authenticator;
import taninim.fb.FbAuthenticator;

import java.time.Duration;
import java.util.concurrent.Executors;

import static com.github.kjetilv.uplift.kernel.Time.utcSupplier;

public final class Main {

    public static void main(String[] args) {
        Flogs.initialize(
            LogLevel.DEBUG
        );

        LambdaClientSettings clientSettings =
            new LambdaClientSettings(ENV, utcSupplier());
        TaninimSettings taninimSettings = new TaninimSettings(
            Duration.ofDays(1),
            Duration.ofHours(1),
            K * K
        );

        S3AccessorFactory s3AccessorFactory =
            new DefaultS3AccessorFactory(ENV, Executors.newVirtualThreadPerTaskExecutor());
        Authenticator fbAuthenticator =
            new FbAuthenticator();

        LambdaHandler handler = YellinLambdaHandler.handler(
            clientSettings,
            taninimSettings,
            s3AccessorFactory,
            fbAuthenticator
        );
        LamdbdaManaged.create(
            ENV.awsLambdaUri(),
            clientSettings,
            handler,
            Executors.newVirtualThreadPerTaskExecutor()
        ).run();

        Flogs.close();
    }

    private static final Env ENV = Env.actual();

    private static final int K = 1024;
}
