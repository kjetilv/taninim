package mediaserver.sessions;

import mediaserver.hash.AbstractHashable;
import mediaserver.util.Print;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class ActiveUser extends AbstractHashable {

    private final Session session;

    private final Instant time;

    private final String name;

    private final String id;

    private final AccessLevel accessLevel;

    private static final long serialVersionUID = 4919694628627926851L;

    public ActiveUser(Session session, Instant time, String name, String id, AccessLevel accessLevel) {

        this.session = Objects.requireNonNull(session, "session");
        this.time = time;
        this.name = Objects.requireNonNull(name, "name");
        this.id = id;
        this.accessLevel = Objects.requireNonNull(accessLevel, "accessLevel");
        if (!accessLevel.satisfies(AccessLevel.LOGIN)) {
            throw new IllegalArgumentException("No login for " + name);
        }
    }

    public String getTimeRequested() {

        return Print.aboutTime(time);
    }

    public String getTimeLeft() {

        return Print.pretty(Duration.between(time, session.getEndTime()));
    }

    public String getMbStreamed() {
        return Print.bytes(session.getStreamedBytes());
    }

    public String getMbQuota() {
        return Print.bytes(session.getStreamQuota());
    }

    public Session getSession() {

        return session;
    }

    public String getName() {

        return name;
    }

    public AccessLevel getAccessLevel() {

        return session.getAccessLevel();
    }

    public String getPrettyAccessLevel() {

        return session.getAccessLevel().name().toLowerCase();
    }

    public String getId() {

        return id;
    }

    public boolean isStreamingCurated() {

        return getAccessLevel().satisfies(AccessLevel.STREAM_CURATED);
    }

    public boolean isStreaming() {

        return getAccessLevel().satisfies(AccessLevel.STREAM);
    }

    public boolean isStreamingPlaylists() {

        return getAccessLevel().satisfies(AccessLevel.STREAM_PLAYLISTS);
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {

        hash(h, name, session.getCookie().toString());
    }

    @Override
    protected StringBuilder withStringBody(StringBuilder sb) {

        return sb.append(name).append('/').append(getAccessLevel().name());
    }
}
