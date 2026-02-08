import module java.base;
import com.github.kjetilv.uplift.flogs.Flogs;
import com.github.kjetilv.uplift.kernel.Env;
import com.github.kjetilv.uplift.lambda.Lambda;
import com.github.kjetilv.uplift.lambda.LambdaClientSettings;
import com.github.kjetilv.uplift.lambda.LambdaHandler;
import com.github.kjetilv.uplift.s3.S3AccessorFactory;
import taninim.TaninimSettings;
import taninim.kudu.DefaultKudu;
import taninim.kudu.KuduLambdaHandler;

import static com.github.kjetilv.uplift.flogs.LogLevel.DEBUG;

void main() {
    Flogs.initialize(DEBUG);

    var env = Env.actual();
    var clientSettings = new LambdaClientSettings(env, Clock.systemUTC()::instant);
    var taninimSettings = new TaninimSettings(
        Duration.ofDays(1),
        Duration.ofHours(4),
        1_024 * 1_024
    );

    var s3 = S3AccessorFactory.defaultFactory(env);
    var kudu = (LambdaHandler) new KuduLambdaHandler(DefaultKudu.create(clientSettings, taninimSettings, s3));
    var uri = env.awsLambdaUri();

    try {
        try (var managed = Lambda.managed(uri, clientSettings, kudu)) {
            managed.run();
        }
    } catch (Exception e) {
        throw new RuntimeException("Failed to close/run", e);
    }
}
