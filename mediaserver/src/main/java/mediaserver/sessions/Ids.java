package mediaserver.sessions;

import mediaserver.externals.FacebookUser;

import java.util.Map;

public final class Ids {

    private final Map<String, ?> sources;

    public Ids(Map<String, ?> sources) {

        this.sources = sources;
    }

    public AccessLevel getLevel(FacebookUser facebookUser) {

        return sources.entrySet().stream()
            .filter(id ->
                String.valueOf(id.getValue()).equals(facebookUser.getId()))
            .map(id -> {
                String key = id.getKey();
                return key.endsWith("**") ? AccessLevel.ADMIN
                    : key.endsWith("*") ? AccessLevel.STREAM
                    : AccessLevel.LOGIN;
            })
            .findFirst()
            .orElse(AccessLevel.NONE);
    }

}
