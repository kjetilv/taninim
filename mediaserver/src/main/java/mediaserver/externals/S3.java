package mediaserver.externals;

import io.minio.MinioClient;
import mediaserver.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

public class S3 {

    private static final String BUCKET = "taninim-water";

    static final Logger log = LoggerFactory.getLogger(S3.class);

    private static final String REGION = "eu-north-1";

    private static final String AMAZONAWS_COM = "https://s3.amazonaws.com/";

    private static final String SELF_ASSIGNED = "http://169.254.170.2";

    private S3() {

    }

    static Optional<S3Client> s3(String cloudUri) {

        URI uri = URI.create(SELF_ASSIGNED + cloudUri);
        log.info("Looking for credentials on {}", uri);
        try {
            HttpURLConnection urlConnection = (HttpURLConnection) uri.toURL().openConnection();
            try (InputStream inputStream = urlConnection.getInputStream()) {
                Map<?, ?> credentials = IO.OM.readerFor(Map.class)
                    .readValue(inputStream);
                log.info(
                    "Connecting to S3 with cloud credentials, access key {}, expiry: {}",
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

    static S3Client newClient(String awsKey, String awsSecret) {

        try {
            MinioClient client = new MinioClient(AMAZONAWS_COM, awsKey, awsSecret, REGION);
            return new DefaultS3Client(client, BUCKET);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to connect to S3", e);
        }
    }

    private static String get(Map<?, ?> credentials, Credentials field) {

        return Optional.ofNullable(credentials.get(field.name())).map(Object::toString)
            .orElseThrow(() -> new IllegalStateException
                ("Could not find " + field + ", keys: " + credentials.keySet()));
    }

    private enum Credentials {

        AccessKeyId,
        SecretAccessKey,
        Token,
        RoleArn,
        Expiration
    }
}
