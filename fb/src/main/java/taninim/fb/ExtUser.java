package taninim.fb;

import module java.base;
import module uplift.json.anno;

import static java.util.Objects.requireNonNull;

@JsonRecord
public record ExtUser(
    String name,
    String id
) {
    public ExtUser {
        requireNonNull(name, "name");
        requireNonNull(id, "id");
    }

    public boolean hasId(String id) {
        return this.id.equals(id);
    }
}
