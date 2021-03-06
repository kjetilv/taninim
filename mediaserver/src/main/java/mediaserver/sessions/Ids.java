package mediaserver.sessions;

import java.util.Arrays;

import mediaserver.externals.ACL;
import mediaserver.externals.FbUser;
import mediaserver.externals.S3Client;
import mediaserver.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Ids {

    private static final Logger log = LoggerFactory.getLogger(Ids.class);

    public static final String IDS_RESOURCE = "ids.json";

    private final ACL acl;

    private final S3Client s3;

    public Ids(ACL acl, S3Client s3) {

        this.acl = acl == null || acl.isEmpty() ? ACL.NONE : acl;
        this.s3 = s3;
    }

    public void persist() {

        s3.put(contents(), IDS_RESOURCE);
        log.info("Uploaded:{}", acl);
    }

    public AccessLevel resolve(FbUser fbUser) {

        return Arrays.stream(acl.getAcl())
            .filter(entry ->
                String.valueOf(entry.getSer()).equals(fbUser.getId()))
            .map(entry ->
                AccessLevel.get(entry.getLev()))
            .findFirst()
            .orElse(AccessLevel.NONE);
    }

    public ACL getACL() {

        return acl;
    }

    private String contents() {

        try {
            return IO.OM.writerFor(ACL.class).writeValueAsString(acl);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write ACL", e);
        }
    }
}
