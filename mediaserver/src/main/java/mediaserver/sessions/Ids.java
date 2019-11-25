package mediaserver.sessions;

import mediaserver.externals.FacebookUser;

import java.util.Map;
import java.util.Optional;

public class Ids {

    private final Map<String, ?> sources;

    private final boolean dev;

    public Ids(Map<String, ?> sources, boolean dev) {

        this.sources = sources;
        this.dev = dev;
    }

    public Optional<String> dev() {

        return dev ? Optional.of("dev") : Optional.empty();
    }

    public boolean authorized(FacebookUser facebookUser) {

        return sources.values().stream().anyMatch(id ->
            String.valueOf(id).equalsIgnoreCase(facebookUser.getId()));
    }
}
