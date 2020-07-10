package mediaserver.externals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.minio.MinioClient;
import io.minio.ObjectStat;
import io.minio.Result;
import io.minio.messages.DeleteError;
import io.minio.messages.Item;

class DefaultS3Client implements S3Client {

    private final MinioClient s3;

    private final String bucket;

    DefaultS3Client(MinioClient s3, String bucket) {

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
            S3.log.info("Giving up on accessing S3: {}", e.toString());
            return Optional.empty();
        } catch (Exception e) {
            if (e.getMessage().equals("Object does not exist")) {
                return Optional.empty();
            }
            throw new IllegalStateException("Could not stat " + name, e);
        }
    }
}
