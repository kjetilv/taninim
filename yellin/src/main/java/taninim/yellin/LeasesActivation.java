package taninim.yellin;

import module java.base;
import com.github.kjetilv.uplift.json.anno.JsonRecord;

import static com.github.kjetilv.uplift.hash.HashKind.K128;

@JsonRecord
public record LeasesActivation(
    String name,
    String userId,
    String token,
    List<String> trackUUIDs,
    Instant expiry
) {

    public LeasesActivation(String name, String userId, List<String> trackUUIDs, Instant expiry) {
        this(name, userId, null, trackUUIDs, expiry);
    }

    public LeasesActivation(String name, String userId, String token, List<String> trackUUIDs, Instant expiry) {
        this.name = name;
        this.userId = userId;
        this.token = token == null ? K128.random().digest() : token;
        this.trackUUIDs = trackUUIDs == null || trackUUIDs.isEmpty() ? Collections.emptyList() : trackUUIDs;
        this.expiry = Objects.requireNonNull(expiry, "expiry");
    }

    @Override
    public List<String> trackUUIDs() {
        return trackUUIDs == null ? Collections.emptyList() : trackUUIDs;
    }

    public boolean isEmpty() {
        return trackUUIDs().isEmpty();
    }

    public int size() {
        return trackUUIDs.size();
    }

    public Optional<String> digest() {
        return Optional.ofNullable(token);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + token + ": " + trackUUIDs().size() + " tracks]";
    }
}
