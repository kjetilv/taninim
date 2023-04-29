package mediaserver.yellin;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.github.kjetilv.uplift.kernel.uuid.Uuid;

public record LeasesActivation(
    String name,
    String userId,
    Uuid token,
    List<Uuid> trackUUIDs
) {

    public LeasesActivation(String name, String userId, Uuid token) {
        this(name, userId, token, null);
    }

    public LeasesActivation(String name, String userId, List<Uuid> trackUUIDs) {
        this(name, userId, null, trackUUIDs);
    }

    public LeasesActivation(String name, String userId, Uuid token, List<Uuid> trackUUIDs) {
        this.name = name;
        this.userId = userId;
        this.token = token == null ? Uuid.random() : token;
        this.trackUUIDs = trackUUIDs == null || trackUUIDs.isEmpty() ? Collections.emptyList() : trackUUIDs;
    }

    @Override
    public List<Uuid> trackUUIDs() {
        return trackUUIDs == null ? Collections.emptyList() : trackUUIDs;
    }

    public boolean isEmpty() {
        return trackUUIDs().isEmpty();
    }

    public LeasesActivation empty() {
        return new LeasesActivation(name, userId, token, null);
    }

    public int size() {
        return trackUUIDs.size();
    }

    public Optional<String> digest() {
        return Optional.ofNullable(token).map(Uuid::digest);
    }

    public LeasesActivation with(Uuid token) {
        return new LeasesActivation(name, userId, token, trackUUIDs);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + token + ": " + trackUUIDs().size() + " tracks]";
    }
}
