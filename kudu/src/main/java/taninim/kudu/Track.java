package taninim.kudu;

import java.util.Locale;
import java.util.Optional;

import com.github.kjetilv.uplift.kernel.uuid.Uuid;

import static com.github.kjetilv.uplift.kernel.io.ParseBits.tailString;

public record Track(
    Uuid trackUUID,
    Format format
) {

    public static Optional<Track> parseTrack(String audioFile) {
        return format(audioFile).map(format ->
            new Track(
                Uuid.from(audioFile),
                format
            ));
    }

    public enum Format {
        FLAC,
        M4A;

        private final String suffix;

        Format() {
            this.suffix = name().toLowerCase();
        }

        public String suffix() {
            return suffix;
        }
    }

    String file() {
        return trackUUID.uuid() + "." + format.suffix();
    }

    private static Optional<Format> format(String audioId) {
        return formatTail(audioId)
            .flatMap(Track::toFormat);
    }

    private static Optional<String> formatTail(String audioId) {
        return tailString(audioId, Uuid.DIGEST_LENGTH + 1);
    }

    private static Optional<Format> toFormat(String tail) {
        String tailLowerCased = tail.toLowerCase(Locale.ROOT);
        return tailLowerCased.startsWith("m4a")
            ? Optional.of(Format.M4A)
            : tailLowerCased.startsWith("flac") ? Optional.of(Format.FLAC) : Optional.empty();
    }
}
