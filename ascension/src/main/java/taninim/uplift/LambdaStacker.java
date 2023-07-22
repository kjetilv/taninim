package taninim.uplift;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.IBucket;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static software.amazon.awscdk.services.lambda.Architecture.ARM_64;
import static software.amazon.awscdk.services.lambda.HttpMethod.*;
import static software.amazon.awscdk.services.logs.RetentionDays.ONE_DAY;

@SuppressWarnings({"unused", "MagicNumber"})
public class LambdaStacker implements Consumer<Stack> {

    public static final String BUCKET_PROPERTY = "taninimBucket";
    private static final String FB_SEC = "fbSec";

    private static final String BUCKET_ENV_VAR = "TANINIM_BUCKET";

    private static IBucket bucket(Stack stack) {
        return Bucket.fromBucketName(stack, "taninim-water-bucket", get(BUCKET_PROPERTY));
    }

    private static void stackYellin(Stack stack, IBucket taninimBucket) {
        IFunction yellinFunction = Function.Builder.create(stack, "yellin-taninim")
                .description("Authoriazation and access control")
                .functionName("yellin-taninim")
                .code(Code.fromAsset("/lambdas/yellin.zip"))
                .environment(Map.of(
                        FB_SEC, get(FB_SEC),
                        BUCKET_ENV_VAR, get(BUCKET_PROPERTY)
                ))
                .handler("bootstrap")
                .logRetention(ONE_DAY)
                .architecture(ARM_64)
                .memorySize(128)
                .runtime(Runtime.PROVIDED_AL2)
                .timeout(Duration.seconds(20))
                .build();
        taninimBucket.grantReadWrite(Objects.requireNonNull(yellinFunction.getRole()));
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
    }

    private static void stackKudu(Stack stack, IBucket taninimBucket) {
        IFunction kuduFunction = Function.Builder.create(stack, "kudu-taninim")
                .description("Serves authorized audio streams")
                .functionName("kudu-taninim")
                .code(Code.fromAsset("/lambdas/kudu.zip"))
                .handler("bootstrap")
                .environment(Map.of(
                        BUCKET_ENV_VAR, get(BUCKET_PROPERTY)
                ))
                .logRetention(ONE_DAY)
                .architecture(ARM_64)
                .memorySize(128)
                .runtime(Runtime.PROVIDED_AL2)
                .timeout(Duration.seconds(20))
                .build();
        taninimBucket.grantRead(Objects.requireNonNull(kuduFunction.getRole()));
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

    private static String get(String property) {
        return Optional.ofNullable(System.getenv(property))
                .filter(value ->
                        !value.isBlank() && !value.equals("null"))
                .orElseThrow(() ->
                        new IllegalStateException(property + " must be set in environment: `" + System.getenv(property) + "`"));
    }

    @Override
    public void accept(Stack stack) {
        IBucket taninimBucket = bucket(stack);
        stackYellin(stack, taninimBucket);
        stackKudu(stack, taninimBucket);
    }
}
