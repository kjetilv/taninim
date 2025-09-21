package taninim.yellin;

import module java.base;
import module taninim.fb;
import module taninim.taninim;
import module uplift.flogs;
import module uplift.kernel;
import module uplift.lambda;
import module uplift.s3;
import module uplift.util;

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
