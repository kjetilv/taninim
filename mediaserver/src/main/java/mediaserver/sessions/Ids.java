package mediaserver.sessions;

import mediaserver.externals.ACL;
import mediaserver.externals.FacebookUser;
import mediaserver.util.IO;
import mediaserver.util.S3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public final class Ids {

    public static final String IDS_RESOURCE = "ids.json";

    private final ACL acl;

    private final S3.Client s3;

    private static final Logger log = LoggerFactory.getLogger(Ids.class);

    public Ids(ACL acl, S3.Client s3) {

        this.acl = acl == null || acl.isEmpty() ? ACL.NONE : acl;
        this.s3 = s3;
    }

    public void persist() {

        s3.put(contents(), IDS_RESOURCE);
        log.info("Uploaded:{}", acl);
    }

    public AccessLevel resolve(FacebookUser facebookUser) {

        return Arrays.stream(acl.getAcl())
            .filter(entry ->
                String.valueOf(entry.getSer()).equals(facebookUser.getId()))
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
