package mediaserver.files;

import mediaserver.hash.AbstractHashable;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

class AbstractNameHashable
    extends AbstractHashable
    implements Comparable<AbstractNameHashable>, Serializable {

    private final String name;

    private final static Map<UUID, AbstractNameHashable> HASHABLES = new ConcurrentHashMap<>();

    private static final long serialVersionUID = 6776617816992974873L;

    AbstractNameHashable(String name) {

        this.name = Objects.requireNonNull(name, "name");
    }

    @Override
    public final void hashTo(Consumer<byte[]> h) {

        hash(h, name, getClass().getName());
    }

    @Override
    public final int compareTo(AbstractNameHashable o) {

        return name.compareTo(o.getName());
    }

    public final String getName() {

        return name;
    }

    @SuppressWarnings("unchecked")
    static <H extends AbstractNameHashable> H get(Function<String, H> ctor, String name) {

        return (H) HASHABLES.computeIfAbsent(
            ctor.apply(
                name.toLowerCase()).getUuid(),
            uuid ->
                ctor.apply(name));
    }

    @Override
    protected StringBuilder withStringBody(StringBuilder sb) {

        return sb.append(name);
    }
}
