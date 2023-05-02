package taninim.fb;

import static java.util.Objects.requireNonNull;

public record ExtUser(
    String name,
    String id
) {

    public ExtUser(String name, String id) {
        this.name = requireNonNull(name, "name");
        this.id = requireNonNull(id, "id");
    }

    public boolean hasId(String id) {
        return this.id.equals(id);
    }
}
