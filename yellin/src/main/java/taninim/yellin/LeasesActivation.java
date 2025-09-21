package taninim.yellin;

import module java.base;
import module uplift.json.anno;
import module uplift.uuid;

@JsonRecord
public record LeasesActivation(
    String name,
    String userId,
    Uuid token,
    List<Uuid> trackUUIDs,
    Instant expiry
) {

    public LeasesActivation(String name, String userId, List<Uuid> trackUUIDs, Instant expiry) {
        this(name, userId, null, trackUUIDs, expiry);
    }

    public LeasesActivation(String name, String userId, Uuid token, List<Uuid> trackUUIDs, Instant expiry) {
        this.name = name;
        this.userId = userId;
        this.token = token == null ? Uuid.random() : token;
        this.trackUUIDs = trackUUIDs == null || trackUUIDs.isEmpty() ? Collections.emptyList() : trackUUIDs;
        this.expiry = Objects.requireNonNull(expiry, "expiry");
    }

    @Override
    public List<Uuid> trackUUIDs() {
        return trackUUIDs == null ? Collections.emptyList() : trackUUIDs;
    }

    public boolean isEmpty() {
        return trackUUIDs().isEmpty();
    }

    public int size() {
        return trackUUIDs.size();
    }

    public Optional<String> digest() {
        return Optional.ofNullable(token)
            .map(Uuid::digest);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + token + ": " + trackUUIDs().size() + " tracks]";
    }
}
