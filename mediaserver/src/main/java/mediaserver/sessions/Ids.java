package mediaserver.sessions;

import mediaserver.externals.FacebookUser;

import java.util.Map;

public final class Ids {

    private final Map<String, ?> sources;

    public Ids(Map<String, ?> sources) {

        this.sources = sources;
    }

    public boolean isAuthorized(FacebookUser facebookUser) {

        return sources.values().stream().anyMatch(id ->
            String.valueOf(id).equalsIgnoreCase(facebookUser.getId()));
    }
}
