package mediaserver.sessions;

import mediaserver.externals.FacebookUser;
import mediaserver.media.CloudMedia;
import mediaserver.util.IO;
import mediaserver.util.S3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class Ids {

    public static final String IDS = "ids.json";

    private final Map<String, ?> sources;

    private static final Logger log = LoggerFactory.getLogger(Ids.class);

    public Ids(Map<String, ?> sources) {

        this.sources = sources;
    }

    public void persist() {

        S3.get().ifPresentOrElse(
            s3 -> {
                try {
                    Path newJson = Files.createTempFile("new-ids", "json");
                    IO.OM.writerFor(Map.class).writeValue(newJson.toFile(), sources);
                    CloudMedia.put(s3, newJson.toFile(), IDS);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to upload", e);
                }
                log.info("Uploaded:{}", sources.keySet());
            },
            () -> {
                log.warn("Not uploading, no S3 connection: {}", sources.keySet());
            });
    }

    public AccessLevel resolve(FacebookUser facebookUser) {

        return sources.entrySet().stream()
            .filter(id ->
                String.valueOf(id.getValue()).equals(facebookUser.getId()))
            .map(id -> {
                String key = id.getKey();
                return key.endsWith("**") ? AccessLevel.ADMIN
                    : key.endsWith("*") ? AccessLevel.STREAM
                    : AccessLevel.LOGIN;
            })
            .findFirst()
            .orElse(AccessLevel.NONE);
    }

    public Map<String, ?> toMap() {

        return Map.copyOf(sources);
    }
}
