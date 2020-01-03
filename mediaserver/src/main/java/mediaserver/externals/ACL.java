package mediaserver.externals;

import java.util.Arrays;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class ACL {

    public static final ACLEntry[] NONE = new ACLEntry[0];

    public ACLEntry[] acl;

    public ACLEntry[] getAcl() {

        return acl.clone();
    }

    public void setAcl(ACLEntry[] acl) {

        this.acl = acl == null || acl.length == 0 ? NONE : acl.clone();
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" +
            Arrays.stream(acl).map(ACLEntry::toString).collect(Collectors.joining(" ")) +
            "]";
    }
}
