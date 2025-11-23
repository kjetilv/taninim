import module java.base;
import com.github.kjetilv.uplift.flogs.Flogs;
import com.github.kjetilv.uplift.flogs.LogLevel;
import com.github.kjetilv.uplift.kernel.Env;
import com.github.kjetilv.uplift.lambda.Lambda;
import com.github.kjetilv.uplift.lambda.LambdaClientSettings;
import com.github.kjetilv.uplift.s3.S3AccessorFactory;
import com.github.kjetilv.uplift.util.Time;
import taninim.TaninimSettings;
import taninim.fb.Authenticator;
import taninim.fb.DefaultFbAuthenticator;
import taninim.yellin.YellinLambdaHandler;

void main() {
    Flogs.initialize(LogLevel.DEBUG);

    var env = Env.actual();

    var clientSettings = new LambdaClientSettings(env, Time.utcSupplier());

    var s3AccessorFactory = S3AccessorFactory.defaultFactory(env);

    var fbAuthenticator = (Authenticator) new DefaultFbAuthenticator();

    var yellin = YellinLambdaHandler.handler(
        clientSettings,
        new TaninimSettings(
            Duration.ofDays(1),
            Duration.ofHours(1),
            1024 * 1024
        ),
        s3AccessorFactory,
        fbAuthenticator
    );

    var uri = env.awsLambdaUri();
    try (
        var managed = Lambda.managed(uri, clientSettings, yellin)
    ) {
        managed.run();
    } catch (Exception e) {
        throw new RuntimeException("Failed to run/close", e);
    }
}
