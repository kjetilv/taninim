package taninim.yellin;

import module uplift.json.anno;
import module uplift.uuid;

@JsonRecord
public record LeasesData(
    String userId,
    Uuid token,
    Uuid album
) {

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + userId + "]";
    }
}
