package mediaserver.gui;

import mediaserver.hash.AbstractHashable;
import mediaserver.sessions.AccessLevel;

import java.util.Objects;
import java.util.function.Consumer;

public class ActiveUser extends AbstractHashable {

    private final String name;

    private final AccessLevel accessLevel;

    private static final long serialVersionUID = 4919694628627926851L;

    public ActiveUser(String name, AccessLevel accessLevel) {

        this.name = Objects.requireNonNull(name, "name");
        this.accessLevel = Objects.requireNonNull(accessLevel, "accessLevel");
        if (accessLevel.ordinal() == 0) {
            throw new IllegalArgumentException("No access level for " + name);
        }
    }

    public String getName() {

        return name;
    }

    public AccessLevel getAccessLevel() {

        return accessLevel;
    }

    public boolean isStreaming() {
        return accessLevel.ordinal() >= AccessLevel.STREAM.ordinal();
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hash(h, name);
        hash(h, accessLevel.ordinal());
    }

    @Override
    protected StringBuilder withStringBody(StringBuilder sb) {

        return sb.append(name).append('/').append(accessLevel.name().toLowerCase());
    }
}
