package taninim.yellin;

import com.github.kjetilv.uplift.json.anno.JsonRecord;
import com.github.kjetilv.uplift.uuid.Uuid;

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
