package mediaserver.externals;

import java.util.Arrays;
import java.util.stream.Collectors;

import mediaserver.util.IO;

@SuppressWarnings("unused")
public class ACL {

    public static final ACL NONE = new ACL();

    private ACLEntry[] acl = NO_ENTRIES;

    public static ACL readLocalACL(String resource) {
        return IO.readUTF8(resource)
            .unpack(res ->
                IO.read(ACL.class, resource, res))
            .orElseThrow(() ->
                new IllegalArgumentException("No resource found @ " + resource));
    }

    public ACLEntry[] getAcl() {
        return acl.clone();
    }

    public void setAcl(ACLEntry[] acl) {
        this.acl = acl == null || acl.length == 0 ? NO_ENTRIES : acl.clone();
    }

    public boolean isEmpty() {
        return this.acl.length == 0;
    }

    private static final ACLEntry[] NO_ENTRIES = new ACLEntry[0];

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" +
            Arrays.stream(acl).map(ACLEntry::toString).collect(Collectors.joining(" ")) +
            "]";
    }
}
