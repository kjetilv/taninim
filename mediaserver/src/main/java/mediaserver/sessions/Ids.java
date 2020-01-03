package mediaserver.sessions;

import mediaserver.externals.ACL;
import mediaserver.externals.FacebookUser;
import mediaserver.media.CloudMedia;
import mediaserver.util.IO;
import mediaserver.util.S3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

public final class Ids {

    public static final String IDS = "ids.json";

    private final ACL sources;

    private static final Logger log = LoggerFactory.getLogger(Ids.class);

    public Ids(ACL acl) {

        this.sources = acl;
        log.info("Ids loaded: {}", sources == null || sources.getAcl().length == 0
            ? "{}"
            : Arrays.toString(sources.getAcl()));
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
                log.info("Uploaded:{}", sources);
            },
            () -> {
                log.warn("Not uploading, no S3 connection: {}", sources);
            });
    }

    public AccessLevel resolve(FacebookUser facebookUser) {

        return Arrays.stream(sources.getAcl())
            .filter(entry ->
                String.valueOf(entry.getSer()).equals(facebookUser.getId()))
            .map(entry ->
                AccessLevel.get(entry.getLev()))
            .findFirst()
            .orElse(AccessLevel.NONE);
    }

    public ACL getSource() {

        return sources;
    }
}
