package mediaserver.externals;

import mediaserver.hash.AbstractHashable;

import java.util.function.Consumer;

public class FacebookUser extends AbstractHashable {

    private final String name;

    private final String id;

    public FacebookUser(String name) {

        this(name, name);
    }

    public FacebookUser(String name, String id) {

        this.name = name;
        this.id = id;
    }

    public String getName() {

        return name;
    }

    public String getId() {

        return id;
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hash(h, id, name);
    }

    @Override
    protected Object toStringBody() {

        return name + ":" + id;
    }
}
