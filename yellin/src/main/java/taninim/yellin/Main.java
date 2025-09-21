package taninim.yellin;

import com.github.kjetilv.uplift.flogs.Flogs;
import com.github.kjetilv.uplift.flogs.LogLevel;
import com.github.kjetilv.uplift.kernel.Env;
import com.github.kjetilv.uplift.lambda.LambdaClientSettings;
import com.github.kjetilv.uplift.lambda.LambdaHandler;
import com.github.kjetilv.uplift.lambda.LamdbdaManaged;
import com.github.kjetilv.uplift.s3.S3AccessorFactory;
import com.github.kjetilv.uplift.util.Time;
import taninim.TaninimSettings;
import taninim.fb.FbAuthenticator;

import java.time.Duration;

public final class Main {

    static void main() {
        Flogs.initialize(LogLevel.DEBUG);

        LambdaClientSettings clientSettings =
            new LambdaClientSettings(ENV, Time.utcSupplier());
        TaninimSettings taninimSettings = new TaninimSettings(
            Duration.ofDays(1),
            Duration.ofHours(1),
            K * K
        );

        S3AccessorFactory s3AccessorFactory = S3AccessorFactory.defaultFactory(ENV);
        FbAuthenticator fbAuthenticator = FbAuthenticator.simple();

        LambdaHandler handler = YellinLambdaHandler.handler(
            clientSettings,
            taninimSettings,
            s3AccessorFactory,
            fbAuthenticator
        );
        try (LamdbdaManaged lamdbdaManaged = LamdbdaManaged.create(ENV.awsLambdaUri(), clientSettings, handler)) {
            lamdbdaManaged.run();
        } catch (Exception e) {
            throw new RuntimeException("Failed to run/close", e);
        }
    }

    private Main() {
    }

    private static final Env ENV = Env.actual();

    private static final int K = 1024;
}
