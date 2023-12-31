package taninim.yellin;

import com.github.kjetilv.uplift.kernel.util.Maps;
import com.github.kjetilv.uplift.uuid.Uuid;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Function;

import static com.github.kjetilv.uplift.kernel.util.Maps.optEntry;
import static java.util.Objects.requireNonNull;

public final class ActivationSerializer {

    private final Function<Object, String> json;

    ActivationSerializer(Function<Object, String> json) {
        this.json = requireNonNull(json, "json");
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

    private static final ZoneId UTC = ZoneId.of("UTC");

    private static List<String> uuids(LeasesActivation activation) {
        return activation.trackUUIDs().stream()
            .map(Uuid::digest)
            .toList();
    }
}
