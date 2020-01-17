package mediaserver.sessions;

import mediaserver.hash.AbstractHashable;
import mediaserver.sessions.AccessLevel;

import java.util.Objects;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class ActiveUser extends AbstractHashable {

    private final String name;

    private final AccessLevel accessLevel;

    private static final long serialVersionUID = 4919694628627926851L;

    public ActiveUser(String name, AccessLevel accessLevel) {

        this.name = Objects.requireNonNull(name, "name");
        this.accessLevel = Objects.requireNonNull(accessLevel, "accessLevel");
        if (!accessLevel.satisfies(AccessLevel.LOGIN)) {
            throw new IllegalArgumentException("No login for " + name);
        }
    }

    public String getName() {

        return name;
    }

    public AccessLevel getAccessLevel() {

        return accessLevel;
    }

    public boolean isStreamingCurated() {

        return accessLevel.satisfies(AccessLevel.STREAM_CURATED);
    }

    public boolean isStreaming() {

        return accessLevel.satisfies(AccessLevel.STREAM);
    }

    public boolean isStreamingPlaylists() {

        return accessLevel.satisfies(AccessLevel.STREAM_PLAYLISTS);
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {

        hash(h, name, accessLevel.name());
    }

    @Override
    protected StringBuilder withStringBody(StringBuilder sb) {

        return sb.append(name).append('/').append(accessLevel.name().toLowerCase());
    }
}
