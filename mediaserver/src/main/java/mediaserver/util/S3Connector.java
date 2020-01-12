package mediaserver.util;

import java.util.*;
import java.util.function.Supplier;

public final class S3Connector {

    private static final String RELATIVE_URI = "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI";

    private static final String AWS_KEY = "awsKey";

    private static final String AWS_SECRET = "awsSecret";

    private static final String AWS_SECRET_ENV = "AWS_SECRET";

    private static final String AWS_KEY_ENV = "AWS_KEY";

    private static final Supplier<Optional<mediaserver.util.S3.Client>> S3 = MostlyOnce.get(S3Connector::s3);

    private S3Connector() {

    }

    public static Optional<mediaserver.util.S3.Client> get() {

        return S3.get();
    }

    private static Optional<mediaserver.util.S3.Client> s3() {

        String awsKey = getProperty(AWS_KEY, AWS_KEY_ENV, true);
        String awsSecret = getProperty(AWS_SECRET, AWS_SECRET_ENV, true);
        String cloudUri = getProperty(RELATIVE_URI, RELATIVE_URI, false);
        if (awsKey != null && awsSecret != null) {
            mediaserver.util.S3.log.info("Connecting to S3 with system properties/env vars");
            return Optional.of(mediaserver.util.S3.newClient(awsKey, awsSecret));
        }
        if (cloudUri != null) {
            return mediaserver.util.S3.s3(cloudUri);
        }
        mediaserver.util.S3.log.warn("No credentials found for S3");
        return Optional.empty();
    }

    private static String getProperty(String property, String envVar, boolean mask) {

        String value = System.getProperty(property, System.getenv(envVar));
        mediaserver.util.S3.log.info("Resolved {}/{} -> {}", property, envVar, mask && value != null
            ? "[" + value.length() + " chars]"
            : value);
        return value;
    }
}
