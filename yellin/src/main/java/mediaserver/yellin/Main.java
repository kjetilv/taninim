package mediaserver.yellin;

import java.time.Duration;

import com.github.kjetilv.uplift.json.Json;
import com.github.kjetilv.uplift.kernel.Env;
import com.github.kjetilv.uplift.lambda.DefaultLamdbdaManaged;
import com.github.kjetilv.uplift.lambda.LambdaClientSettings;
import com.github.kjetilv.uplift.lambda.LambdaHandler;
import com.github.kjetilv.uplift.s3.DefaultS3AccessorFactory;
import com.github.kjetilv.uplift.s3.S3AccessorFactory;
import mediaserver.fb.Authenticator;
import mediaserver.fb.FbAuthenticator;
import mediaserver.taninim.TaninimSettings;

import static com.github.kjetilv.uplift.kernel.ManagedExecutors.executor;
import static com.github.kjetilv.uplift.kernel.Time.utcSupplier;

public final class Main {

    public static void main(String[] args) {
        LambdaClientSettings clientSettings = new LambdaClientSettings(
            ENV,
            Duration.ofMinutes(1),
            executor("L", 10),
            executor("S", 10),
            utcSupplier()
        );
        TaninimSettings taninimSettings = new TaninimSettings(
            Duration.ofDays(1),
            Duration.ofHours(1),
            1024 * 1024
        );
        S3AccessorFactory s3AccessorFactory =
            new DefaultS3AccessorFactory(ENV, executor("S3", 10));
        Authenticator fbAuthenticator = new FbAuthenticator(Json.STRING_2_JSON_MAP);

        LambdaHandler handler = YellinLambdaHandler.handler(
            clientSettings,
            taninimSettings,
            s3AccessorFactory,
            fbAuthenticator
        );
        new DefaultLamdbdaManaged(
            ENV.awsLambdaUri(),
            clientSettings,
            handler
        ).run();
    }

    private static final Env ENV = Env.actual();
}
