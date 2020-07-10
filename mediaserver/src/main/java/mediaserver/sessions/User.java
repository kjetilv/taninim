package mediaserver.sessions;

import mediaserver.hash.AbstractHashable;
import mediaserver.util.DAC;
import mediaserver.util.Print;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Consumer;

public class User extends AbstractHashable {

    private final Session session;

    private final Instant time;

    private final String name;

    private final String id;

    private static final long serialVersionUID = 4919694628627926851L;

    User(Session session, Instant time, String name, String id) {

        this.session = Objects.requireNonNull(session, "session");
        this.time = time;
        this.name = Objects.requireNonNull(name, "name");
        this.id = id;
        AccessLevel accessLevel1 = Objects.requireNonNull(session.getAccessLevel(), "accessLevel");
        if (!accessLevel1.satisfies(AccessLevel.LOGIN)) {
            throw new IllegalArgumentException("No login for " + name);
        }
    }

    @DAC
    public String getTimeRequested() {

        return Print.aboutTime(time);
    }

    @DAC
    public String getTimeLeft() {

        return Print.prettyLongTime(Duration.between(time, session.getEndTime()));
    }

    @DAC
    public String getMbStreamed() {

        return Print.bytes(session.getStreamedBytes());
    }

    @DAC
    public String getMbQuota() {

        return Print.bytes(session.getStreamQuota());
    }

    public Session getSession() {

        return session;
    }

    public String getName() {

        return name;
    }

    @DAC
    public String getPrettyAccessLevel() {

        return session.getAccessLevel().getDescription();
    }

    public String getId() {

        return id;
    }

    @DAC
    public boolean isStreamingCurated() {

        return getAccessLevel().satisfies(AccessLevel.STREAM_CURATED);
    }

    public boolean isStreaming() {

        return getAccessLevel().satisfies(AccessLevel.STREAM);
    }

    @DAC
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

    private AccessLevel getAccessLevel() {

        return session.getAccessLevel();
    }
}
