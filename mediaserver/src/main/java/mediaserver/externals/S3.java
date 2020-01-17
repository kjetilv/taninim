package mediaserver.externals;

import io.minio.MinioClient;
import io.minio.ObjectStat;
import io.minio.Result;
import io.minio.messages.DeleteError;
import io.minio.messages.Item;
import mediaserver.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

public class S3 {

    public static final String BUCKET = "taninim-water";

    static final Logger log = LoggerFactory.getLogger(S3.class);

    private static final String REGION = "eu-north-1";

    private static final String AMAZONAWS_COM = "https://s3.amazonaws.com/";

    private static final String SELF_ASSIGNED = "http://169.254.170.2";

    static Optional<Client> s3(String cloudUri) {

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

    static Client newClient(String awsKey, String awsSecret) {

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

    public interface Client {

        InputStream stream(String name, Long offset, Long length);

        InputStream stream(String name);

        void put(String contents, String remoteName);

        void put(File localFile, String remoteName);

        void remove(Collection<String> objects);

        Map<String, Long> remoteSizes();

        Optional<Instant> lastModifiedRemote(String name);

        Optional<Long> length(String name);

        void put(InputStream inputStream, long length, String remoteName);
    }

    private static class DefaultS3Client implements Client {

        private final MinioClient s3;

        private final String bucket;

        private DefaultS3Client(MinioClient s3, String bucket) {

            this.s3 = s3;
            this.bucket = bucket;
        }

        @Override
        public InputStream stream(String name, Long offset, Long length) {

            try {
                return offset == null || length == null
                    ? s3.getObject(bucket, name)
                    : s3.getObject(bucket, name, offset, length);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load " + name, e);
            }
        }

        @Override
        public InputStream stream(String name) {

            try {
                return s3.getObject(bucket, name);
            } catch (Exception e) {
                throw new IllegalStateException("Could not download " + name, e);
            }
        }

        @Override
        public void put(String contents, String remoteName) {

            byte[] bytes = contents.getBytes(StandardCharsets.UTF_8);
            put(new ByteArrayInputStream(bytes), bytes.length, remoteName);
        }

        @Override
        public void put(File file, String remoteName) {

            try {
                s3.putObject(
                    bucket,
                    remoteName,
                    file.getAbsolutePath(),
                    file.length(),
                    null,
                    null,
                    null);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to put " + file.getName() + " -> " + remoteName, e);
            }
        }

        @Override
        public void remove(Collection<String> names) {

            Iterable<Result<DeleteError>> results = s3.removeObjects(bucket, names);
            if (results.iterator().hasNext()) {
                Collection<String> errors = new ArrayList<>();
                results.forEach(deleteErrorResult -> {
                    try {
                        errors.add(deleteErrorResult.get().getString());
                    } catch (Exception e) {
                        errors.add(deleteErrorResult.toString());
                    }
                });
                if (!errors.isEmpty()) {
                    throw new IllegalStateException(
                        "Failed to remove " + names.size() + " objects, " + errors.size() + "errors: " +
                            String.join(", ", errors));
                }
            }
        }

        @Override
        public Map<String, Long> remoteSizes() {

            Iterable<Result<Item>> remoteFiles;
            try {
                remoteFiles = s3.listObjects(bucket);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to list sizes", e);
            }
            Map<String, Long> items = new HashMap<>();
            remoteFiles.forEach(remoteItem -> {
                try {
                    Item item = remoteItem.get();
                    items.put(item.objectName(), item.objectSize());
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to stat " + items, e);
                }
            });
            return items;
        }

        @Override
        public Optional<Instant> lastModifiedRemote(String name) {

            return objectStat(name)
                .map(ObjectStat::createdTime)
                .map(Date::toInstant);
        }

        @Override
        public Optional<Long> length(String name) {

            return objectStat(name)
                .map(ObjectStat::length);
        }

        @Override
        public void put(InputStream inputStream, long length, String remoteName) {

            try {
                s3.putObject(
                    bucket,
                    remoteName,
                    inputStream,
                    length,
                    null,
                    null,
                    null);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to put -> " + remoteName, e);
            }
        }

        private Optional<ObjectStat> objectStat(String name) {

            try {
                return Optional.ofNullable(s3.statObject(bucket, name));
            } catch (UnknownHostException e) {
                log.info("Giving up on accessing S3: {}", e.toString());
                return Optional.empty();
            } catch (Exception e) {
                if (e.getMessage().equals("Object does not exist")) {
                    return Optional.empty();
                }
                throw new IllegalStateException("Could not stat " + name, e);
            }
        }
    }
}
