package mediaserver;

import io.minio.MinioClient;
import mediaserver.files.Media;
import mediaserver.util.S3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class CloudMedia {

    private static final Logger log = LoggerFactory.getLogger(CloudMedia.class);

    private static final String MEDIA_SER = "media.ser";

    public static void main(String[] args) {

        Media media = Media.local(args[0]);
        File file = serialize(media);
        S3.get().ifPresent(s3 -> {
            uploadMediaSer(media, file, s3);
        });
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
