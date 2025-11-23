package taninim.yellin;

import module java.base;
import com.github.kjetilv.uplift.json.anno.JsonRecord;

@JsonRecord
public record LeasesData(String userId, String token, String album) {

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + userId + "]";
    }
}
