package taninim.yellin;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.github.kjetilv.uplift.kernel.util.Maps;
import com.github.kjetilv.uplift.kernel.uuid.Uuid;

import static com.github.kjetilv.uplift.kernel.util.Maps.optEntry;
import static java.util.Objects.requireNonNull;

public final class ActivationSerializer {

    private final Function<Object, String> json;

    private final BiFunction<String, Duration, Optional<URI>> presigner;

    ActivationSerializer(Function<Object, String> json, BiFunction<String, Duration, Optional<URI>> presigner) {
        this.json = requireNonNull(json, "json");
        this.presigner = requireNonNull(presigner, "presigner");
    }

    public byte[] jsonBody(LeasesActivation activation) {
        return json.apply(
            Maps.fromOptEntries(
                optEntry("name", activation.name()),
                optEntry("userId", activation.userId()),
                optEntry("token", activation.digest().orElse(null)),
                optEntry("trackUUIDs", uuids(activation)),
                optEntry("expiry", activation.expiry().atZone(UTC).toEpochSecond())
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

    private static final ZoneId UTC = ZoneId.of("UTC");

    private static List<String> uuids(LeasesActivation activation) {
        return activation.trackUUIDs().stream()
            .map(Uuid::digest)
            .toList();
    }
}
