package mediaserver.yellin;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.github.kjetilv.uplift.kernel.util.Maps;
import com.github.kjetilv.uplift.kernel.uuid.Uuid;

import static java.util.Objects.requireNonNull;

public final class ActivationSerializer {

    private final Function<Object, String> json;

    private final BiFunction<String, Duration, Optional<URI>> presigner;

    ActivationSerializer(Function<Object, String> json, BiFunction<String, Duration, Optional<URI>> presigner) {
        this.json = requireNonNull(json, "json");
        this.presigner = requireNonNull(presigner, "presigner");
    }

    public byte[] jsonBody(LeasesActivation activation) {
        return json.apply(Maps.toMap(
                Maps.ent("name", activation.name()),
                Maps.ent("userId", activation.userId()),
                Maps.ent("token", activation.digest()),
                Maps.ent("trackUUIDs", uuids(activation))
            )
        ).getBytes(StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unused")
    private Map.Entry<String, List<Optional<URI>>> urlEntry(LeasesActivation activation) {
        return Map.entry("trackURLs", urls(activation));
    }

    private List<Optional<URI>> urls(LeasesActivation activation) {
        return activation.trackUUIDs().stream()
            .map(digest ->
                presigner.apply(digest.uuid().toString() + ".m4a", Duration.ofHours(1)))
            .toList();
    }

    private static List<String> uuids(LeasesActivation activation) {
        return activation.trackUUIDs().stream()
            .map(Uuid::digest)
            .toList();
    }
}
