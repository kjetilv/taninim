package mediaserver.sessions;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class Ids {

    private final Map<String, ?> sources;

    private final boolean dev;

    public Ids(Map<String, ?> sources) {

        this(sources, false);
    }

    public Ids(Map<String, ?> sources, boolean dev) {

        this.sources = sources;
        this.dev = dev;
    }

    public Optional<String> id(String name) {

        return Optional.ofNullable(sources.get(name)).map(String::valueOf);
    }

    public Optional<String> dev() {

        return dev ? Optional.of("dev") : Optional.empty();
    }

    public Collection<String> ids() {

        return sources.keySet().stream().map(String::valueOf).collect(Collectors.toList());
    }
}
