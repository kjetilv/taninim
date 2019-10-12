package mediaserver.files;

import mediaserver.hash.AbstractHashable;

import java.util.Objects;
import java.util.function.Consumer;

public class Artist extends AbstractHashable implements Comparable<Artist> {

    private final String name;

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
