import module java.base;
import module taninim.fb;
import module taninim.taninim;
import module uplift.flogs;
import module uplift.kernel;
import module uplift.lambda;
import module uplift.s3;
import module uplift.util;
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
