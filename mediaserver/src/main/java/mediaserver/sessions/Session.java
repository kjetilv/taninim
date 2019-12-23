package mediaserver.sessions;

import mediaserver.externals.FacebookUser;

import java.time.Instant;
import java.util.UUID;

public final class Session {

    private final Instant cutoff;

    private final UUID cookie;

    private final FacebookUser facebookUser;

    public Session(FacebookUser facebookUser, UUID cookie, Instant cutoff) {

        this.facebookUser = facebookUser;
        this.cookie = cookie;
        this.cutoff = cutoff;
    }

    public UUID getCookie() {

        return cookie;
    }

    public FacebookUser getFacebookUser() {

        return facebookUser;
    }

    public boolean timedout(Instant time) {

        return time.isAfter(cutoff);
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + facebookUser + " -> " + cutoff + " / " + cookie + "]";
    }
}
