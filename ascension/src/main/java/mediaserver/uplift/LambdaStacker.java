package mediaserver.uplift;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionUrl;
import software.amazon.awscdk.services.lambda.FunctionUrlAuthType;
import software.amazon.awscdk.services.lambda.FunctionUrlCorsOptions;
import software.amazon.awscdk.services.lambda.IFunction;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.IBucket;

import static software.amazon.awscdk.services.lambda.Architecture.ARM_64;
import static software.amazon.awscdk.services.lambda.HttpMethod.DELETE;
import static software.amazon.awscdk.services.lambda.HttpMethod.GET;
import static software.amazon.awscdk.services.lambda.HttpMethod.POST;
import static software.amazon.awscdk.services.logs.RetentionDays.ONE_DAY;

@SuppressWarnings("unused")
public class LambdaStacker implements Consumer<Stack> {

    @Override
    public void accept(Stack stack) {
        IBucket taninimBucket = Bucket.fromBucketName(stack, "taninim-water-bucket", "taninim-water");

        IFunction yellinFunction = Function.Builder.create(stack, "yellin-taninim")
            .functionName("yellin-taninim")
            .code(Code.fromAsset("/lambdas/yellin.zip"))
            .environment(Map.of(
                FB_SEC, fbSec()
            ))
            .handler("bootstrap")
            .logRetention(ONE_DAY)
            .architecture(ARM_64)
            .memorySize(128)
            .runtime(Runtime.PROVIDED_AL2)
            .timeout(Duration.seconds(20))
            .build();
        taninimBucket.grantReadWrite(Objects.requireNonNull(yellinFunction.getRole()));

        IFunction kuduFunction = Function.Builder.create(stack, "kudu-taninim")
            .functionName("kudu-taninim")
            .code(Code.fromAsset("/lambdas/kudu.zip"))
            .handler("bootstrap")
            .logRetention(ONE_DAY)
            .architecture(ARM_64)
            .memorySize(128)
            .runtime(Runtime.PROVIDED_AL2)
            .timeout(Duration.seconds(20))
            .build();
        taninimBucket.grantRead(Objects.requireNonNull(kuduFunction.getRole()));

        FunctionUrl.Builder.create(stack, "yellin-taninim-fu")
            .function(yellinFunction)
            .authType(FunctionUrlAuthType.NONE)
            .cors(FunctionUrlCorsOptions.builder()
                .allowedMethods(List.of(POST, DELETE))
                .allowedOrigins(List.of("https://tanin.im:5173", "https://kjetilv.github.io"))
                .allowedHeaders(List.of("Content-Type"))
                .maxAge(Duration.days(1))
                .allowCredentials(true)
                .build())
            .build();

        FunctionUrl.Builder.create(stack, "kudu-taninim-fu")
            .function(kuduFunction)
            .authType(FunctionUrlAuthType.NONE)
            .cors(FunctionUrlCorsOptions.builder()
                .allowedMethods(List.of(GET))
                .allowedOrigins(List.of("https://tanin.im:5173", "https://kjetilv.github.io"))
                .allowedHeaders(List.of("Content-Type", "Range"))
                .maxAge(Duration.days(1))
                .allowCredentials(true)
                .build())
            .build();
    }

    private static final String FB_SEC = "fbSec";

    private static String fbSec() {
        return Optional.ofNullable(System.getenv(FB_SEC))
            .filter(value ->
                !value.isBlank() && !value.equals("null"))
            .orElseThrow(() ->
                new IllegalStateException(FB_SEC + " must be set in environment: `" + System.getenv(FB_SEC) + "`"));
    }
}
