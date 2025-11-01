import module java.base;
import com.github.kjetilv.uplift.flogs.Flogs;
import com.github.kjetilv.uplift.flogs.LogLevel;
import com.github.kjetilv.uplift.kernel.Env;
import com.github.kjetilv.uplift.lambda.LambdaClientSettings;
import com.github.kjetilv.uplift.lambda.LamdbdaManaged;
import com.github.kjetilv.uplift.s3.S3AccessorFactory;
import com.github.kjetilv.uplift.util.Time;
import taninim.TaninimSettings;
import taninim.fb.FbAuthenticator;
import taninim.yellin.YellinLambdaHandler;

void main() {
    Flogs.initialize(LogLevel.DEBUG);

    var clientSettings = new LambdaClientSettings(ENV, Time.utcSupplier());
    var taninimSettings = new TaninimSettings(
        Duration.ofDays(1),
        Duration.ofHours(1),
        K * K
    );

    var s3AccessorFactory = S3AccessorFactory.defaultFactory(ENV);
    var fbAuthenticator = FbAuthenticator.simple();

    var handler = YellinLambdaHandler.handler(
        clientSettings,
        taninimSettings,
        s3AccessorFactory,
        fbAuthenticator
    );
    try (var lamdbdaManaged = LamdbdaManaged.create(ENV.awsLambdaUri(), clientSettings, handler)) {
        lamdbdaManaged.run();
    } catch (Exception e) {
        throw new RuntimeException("Failed to run/close", e);
    }
}

private static final Env ENV = Env.actual();

private static final int K = 1024;
