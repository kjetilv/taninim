package taninim.yellin;

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
        return json.apply(Maps.fromOptionalEntries(
                Maps.optionalEntry("name", activation.name()),
                Maps.optionalEntry("userId", activation.userId()),
                Maps.optionalEntry("token", activation.digest()),
                Maps.optionalEntry("trackUUIDs", uuids(activation))
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
