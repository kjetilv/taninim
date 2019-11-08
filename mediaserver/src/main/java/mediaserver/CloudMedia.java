package mediaserver;

import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import mediaserver.util.S3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class CloudMedia {

    private static final Logger log = LoggerFactory.getLogger(CloudMedia.class);

    private static final String MEDIA_SER = "media.ser";

    public static void main(String[] args) {

        Media media = Media.local(args[0], args[1], args[2]);
        File mediaFile = serialize(media);
        S3.get().ifPresent(s3 -> {
            uploadMediaSer(media, mediaFile, s3);
        });
        if (args.length > 3) {
            File objectsDir = new File(args[1]);
            S3.get().ifPresent(s3 -> {
                Map<String, Long> remoteSizes = remoteFiles(s3);
                Collection<File> localFiles = Optional.ofNullable(
                    objectsDir.listFiles(pathname -> pathname.getName().endsWith(".flac"))
                ).stream().flatMap(Arrays::stream).collect(Collectors.toList());
                localFiles.forEach(localFile -> {
                    Optional<Long> remoteSize =
                        Optional.ofNullable(remoteSizes.get(localFile.getName()));
                    if (remoteSize.filter(size -> size != localFile.length()).isPresent()) {
                        log.info("Uploading {}", localFile.getName());
                        try {
                            s3.putObject(
                                S3.BUCKET,
                                localFile.getName(),
                                localFile.getAbsolutePath(),
                                localFile.length(),
                                null,
                                null,
                                null);
                        } catch (Exception e) {
                            throw new IllegalStateException("Failed to put " + localFile.getName(), e);
                        }
                    } else {
                        log.info("Already present: {}", localFile.getName());
                    }
                });
                Collection<String> localFileNames =
                    localFiles.stream().map(File::getName).collect(Collectors.toSet());
                List<String> removables = remoteSizes.keySet().stream()
                    .filter(remoteName ->
                                !localFileNames.contains(remoteName))
                    .collect(Collectors.toList());
                if (!removables.isEmpty()) {
                    log.info("Removables: {}", removables.size());
                    s3.removeObjects(S3.BUCKET, removables);
                }
            });
        }
    }

    public static Map<String, Long> remoteFiles(MinioClient s3) {

        Iterable<Result<Item>> remoteFiles;
        try {
            remoteFiles = s3.listObjects(S3.BUCKET);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to list " + S3.BUCKET, e);
        }
        Map<String, Long> items = new HashMap<>();
        remoteFiles.forEach(remoteItem -> {
            try {
                Item item = remoteItem.get();
                items.put(
                    item.objectName(),
                    item.objectSize());
            } catch (Exception e) {
                throw new IllegalStateException("Failed to stat " + items, e);
            }
        });
        return items;
    }

    public static void uploadMediaSer(Media media, File file, MinioClient s3) {

        try {
            s3.putObject(
                S3.BUCKET,
                MEDIA_SER,
                file.getAbsolutePath(),
                file.length(),
                null,
                null,
                null);
        } catch (Exception e) {
            throw new IllegalStateException("Failed upload: " + media, e);

        }
    }

    static Media download() {

        InputStream inputStream = S3.get().map(s3 -> {
            try {
                return s3.getObject(S3.BUCKET, MEDIA_SER);
            } catch (Exception e) {
                throw new IllegalStateException("Could not download media file", e);
            }
        }).orElseThrow(() ->
                           new IllegalStateException("No S3 connection"));
        log.info("Downloading media... ");
        Media media = deserialize(inputStream);
        log.info("Downloaded media {}", media);
        return media;
    }

    static Media deserialize(InputStream inputStream) {

        try (
            BufferedInputStream bis = new BufferedInputStream(inputStream);
            ObjectInputStream ois = new ObjectInputStream(bis)
        ) {
            return (Media) ois.readObject();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read media", e);
        }
    }

    private static File serialize(Media media) {

        try {
            Path tmp = Files.createTempFile
                ("media-" + UUID.randomUUID(), "ser");
            try (
                FileOutputStream fos = new FileOutputStream(tmp.toFile());
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                ObjectOutputStream oos = new ObjectOutputStream(bos)
            ) {
                oos.writeObject(media);
            }
            return tmp.toFile();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate upload", e);
        }
    }
}
