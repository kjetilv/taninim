import module java.base;
import module taninim.taninim;
import module uplift.flogs;
import module uplift.kernel;
import module uplift.lambda;
import module uplift.s3;
import taninim.kudu.KuduLambdaHandler;

import static com.github.kjetilv.uplift.flogs.LogLevel.DEBUG;

void main() {
    Flogs.initialize(DEBUG);

    LambdaClientSettings clientSettings =
        new LambdaClientSettings(ENV, Clock.systemUTC()::instant);

    TaninimSettings taninimSettings =
        new TaninimSettings(A_DAY, FOUR_HOURS, K * K);

    S3AccessorFactory s3 = S3AccessorFactory.defaultFactory(ENV);

    LambdaHandler lambdaHandler = KuduLambdaHandler.create(clientSettings, taninimSettings, s3);
    try (LamdbdaManaged lamdbdaManaged = LamdbdaManaged.create(ENV.awsLambdaUri(), clientSettings, lambdaHandler)) {
        lamdbdaManaged.run();
    } catch (Exception e) {
        throw new RuntimeException("Failed to close/run", e);
    }
}

private static final Duration A_DAY = Duration.ofDays(1);

private static final Duration FOUR_HOURS = Duration.ofHours(4);

private static final Env ENV = Env.actual();

private static final int K = 1_024;
