package mediaserver.externals;

import java.util.Arrays;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class ACL {

    public static final ACL NONE = new ACL();

    private static final ACLEntry[] NO_ENTRIES = new ACLEntry[0];

    private ACLEntry[] acl = NO_ENTRIES;

    public ACLEntry[] getAcl() {

        return acl.clone();
    }

    public void setAcl(ACLEntry[] acl) {

        this.acl = acl == null || acl.length == 0 ? NO_ENTRIES : acl.clone();
    }

    public boolean isEmpty() {

        return this.acl.length == 0;
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" +
            Arrays.stream(acl).map(ACLEntry::toString).collect(Collectors.joining(" ")) +
            "]";
    }
}
