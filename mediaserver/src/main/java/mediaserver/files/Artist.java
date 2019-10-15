package mediaserver.files;

import mediaserver.hash.AbstractHashable;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Consumer;

public class Artist extends AbstractHashable
    implements Comparable<Artist>, Serializable {

    private final String name;

    private static final long serialVersionUID = 8396940978009264692L;

    public Artist(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hash(h, name);
    }

    @Override
    public int compareTo(Artist o) {
        return name.compareTo(o.getName());
    }

    public String getName() {
        return name;
    }

    @Override
    protected Object toStringBody() {
        return name;
    }
}
