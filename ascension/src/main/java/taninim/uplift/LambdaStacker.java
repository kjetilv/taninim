package taninim.uplift;

import org.jetbrains.annotations.NotNull;
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

    @Override
    public void accept(Stack stack) {
        IBucket taninimBucket = bucket(stack);
        stackYellin(stack, taninimBucket);
        stackKudu(stack, taninimBucket);
    }

    public static final String BUCKET_PROPERTY = "taninimBucket";

    private static final String FB_SEC = "fbSec";

    private static final String BUCKET_ENV_VAR = "TANINIM_BUCKET";

    private static void stackYellin(Stack stack, IBucket taninimBucket) {
        IFunction yellin = complete(functionBuilder(stack, "yellin-taninim")
            .description("Authorization and access control")
            .functionName("yellin-taninim")
            .code(Code.fromAsset("/lambdas/yellin.zip"))
            .environment(Map.of(
                FB_SEC, get(FB_SEC),
                BUCKET_ENV_VAR, get(BUCKET_PROPERTY)
            )));
        taninimBucket.grantReadWrite(Objects.requireNonNull(yellin.getRole()));
        functionUrl(stack, "yellin-taninim-fu", yellin)
            .cors(corsOptions(
                List.of(POST, DELETE),
                List.of("Content-Type")
            ))
            .build();
    }

    private static void stackKudu(Stack stack, IBucket taninimBucket) {
        IFunction kudu = complete(functionBuilder(stack, "kudu-taninim")
            .description("Serves authorized audio streams")
            .functionName("kudu-taninim")
            .code(Code.fromAsset("/lambdas/kudu.zip"))
            .environment(Map.of(
                BUCKET_ENV_VAR, get(BUCKET_PROPERTY)
            ))
            .logRetention(ONE_DAY));
        taninimBucket.grantRead(Objects.requireNonNull(kudu.getRole()));
        functionUrl(stack, "kudu-taninim-fu", kudu)
            .authType(FunctionUrlAuthType.NONE)
            .cors(corsOptions(
                List.of(GET),
                List.of("Content-Type", "Range")
            ))
            .build();
    }

    @NotNull
    private static Function.Builder functionBuilder(Stack stack, String id) {
        return Function.Builder.create(stack, id);
    }

    private static IFunction complete(Function.Builder builder) {
        IFunction yellinFunction = builder
            .handler("bootstrap")
            .architecture(ARM_64)
            .memorySize(128)
            .runtime(Runtime.PROVIDED_AL2)
            .timeout(Duration.seconds(20))
            .build();
        return yellinFunction;
    }

    private static FunctionUrl.Builder functionUrl(Stack stack, String id, IFunction kuduFunction) {
        return FunctionUrl.Builder
            .create(stack, id).function(kuduFunction)
            .authType(FunctionUrlAuthType.NONE);
    }

    private static FunctionUrlCorsOptions corsOptions(List<HttpMethod> methods, List<String> headers) {
        FunctionUrlCorsOptions.Builder builder = FunctionUrlCorsOptions.builder()
            .allowedMethods(methods)
            .allowedOrigins(List.of(
                "https://tanin.im:5173",
                "https://kjetilv.github.io"
            ))
            .maxAge(Duration.days(1))
            .allowCredentials(false);
        if (headers != null && !headers.isEmpty()) {
            builder.allowedHeaders(headers);
        }
        return builder.build();
    }

    private static String get(String property) {
        return Optional.ofNullable(System.getenv(property))
            .filter(value ->
                !value.isBlank() && !value.equals("null"))
            .orElseThrow(() ->
                new IllegalStateException(property + " must be set in environment: `" + System.getenv(property) + "`"));
    }

    private static IBucket bucket(Stack stack) {
        return Bucket.fromBucketName(stack, "taninim-water-bucket", get(BUCKET_PROPERTY));
    }
}
