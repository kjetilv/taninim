package mediaserver.hash;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class AbstractNameHashable
    extends AbstractHashable
    implements Comparable<AbstractNameHashable>, Serializable, Namable {

    @SuppressWarnings("unchecked")
    public static <H extends AbstractNameHashable> H get(Function<String, H> ctor, String name) {
        return (H) HASHABLES.computeIfAbsent(
            ctor.apply(
                name.toLowerCase()).getUuid(),
            uuid ->
                ctor.apply(name));
    }

    private final String name;

    protected AbstractNameHashable(String name) {
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

    @Override
    public final String getName() {
        return name;
    }

    @Override
    protected StringBuilder withStringBody(StringBuilder sb) {
        return sb.append(name);
    }

    private final static Map<UUID, AbstractNameHashable> HASHABLES = new ConcurrentHashMap<>();

    @Serial private static final long serialVersionUID = 6776617816992974873L;
}
