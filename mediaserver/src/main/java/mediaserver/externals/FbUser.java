package mediaserver.externals;

import java.util.function.Consumer;

import mediaserver.hash.AbstractHashable;

public final class FbUser extends AbstractHashable {

    private final String name;

    private final String id;

    public FbUser(String name, String id) {

        this.name = name;
        this.id = id;
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {

        hash(h, id, name);
    }

    @Override
    protected StringBuilder withStringBody(StringBuilder sb) {

        return sb.append(name).append(":").append(id);
    }

    public String getName() {

        return name;
    }

    public String getId() {

        return id;
    }

    private static final long serialVersionUID = 6897348983612649711L;
}
