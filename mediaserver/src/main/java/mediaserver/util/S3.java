package mediaserver.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class S3 {

    public static final String BUCKET = "taninim-water";

    private static final Logger log = LoggerFactory.getLogger(S3.class);

    private static final String REGION = "eu-north-1";

    private static final String AMAZONAWS_COM = "https://s3.amazonaws.com/";

    private static final String SELF_ASSIGNED = "http://169.254.170.2";

    private static final String RELATIVE_URI = "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI";

    private static final String AWS_KEY = "awsKey";

    private static final String AWS_SECRET = "awsSecret";

    private static final String AWS_SECRET_ENV = "AWS_SECRET";

    private static final String AWS_KEY_ENV = "AWS_KEY";

    private static final Supplier<Optional<MinioClient>> S3 = MostlyOnce.get(mediaserver.util.S3::s3);

    private S3() {

    }

    public static Optional<MinioClient> get() {
        return S3.get();
    }

    private static Optional<MinioClient> s3() {
        String awsKey = getProperty(AWS_KEY, AWS_KEY_ENV, true);
        String awsSecret = getProperty(AWS_SECRET, AWS_SECRET_ENV, true);
        if (awsKey != null && awsSecret != null) {
            log.info("Connecting to S3 with system properties/env vars");
            return Optional.of(newClient(awsKey, awsSecret));
        }
        String cloudUri = getProperty(RELATIVE_URI, RELATIVE_URI, false);
        if (cloudUri != null) {
            URI uri = URI.create(SELF_ASSIGNED + cloudUri);
            log.info("Looking for credentials on {}", uri);
            try {
                HttpURLConnection urlConnection = (HttpURLConnection) uri.toURL().openConnection();
                try (InputStream inputStream = urlConnection.getInputStream()) {
                    Map<?, ?> credentials = new ObjectMapper().readerFor(LinkedHashMap.class)
                        .readValue(inputStream);
                    log.info("Connecting to S3 with cloud credentials, access key {}, expiry: {}",
                        get(credentials, Credentials.AccessKeyId),
                        get(credentials, Credentials.Expiration));
                    return Optional.of(newClient(
                        get(credentials, Credentials.AccessKeyId),
                        get(credentials, Credentials.SecretAccessKey)));
                } finally {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                throw new IllegalStateException("Failed to init from " + cloudUri, e);
            }
        }
        log.warn("No credentials found for S3");
        return Optional.empty();
    }

    private static String get(Map<?, ?> credentials, Credentials field) {
        return Optional.ofNullable(credentials.get(field.name())).map(Object::toString)
            .orElseThrow(() -> new IllegalStateException
                ("Could not find " + field + ", keys: " + credentials.keySet()));
    }

    private static String getProperty(String property, String envVar, boolean mask) {
        String value = System.getProperty(property, System.getenv(envVar));
        log.info("Resolved {}/{} -> {}", property, envVar, mask && value != null
            ? "[" + value.length() + " chars]"
            : value);
        return value;
    }

    private static MinioClient newClient(String awsKey, String awsSecret) {
        try {
            return new MinioClient(
                AMAZONAWS_COM,
                awsKey,
                awsSecret,
                REGION);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to connect to S3", e);
        }
    }

    public enum Credentials {

        AccessKeyId,
        SecretAccessKey,
        Token,
        RoleArn,
        Expiration
    }
}
